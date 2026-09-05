"""
STEP 3 of 3 — THE FULL MINI-GPT (a real, tiny Transformer)
===========================================================

Run me with:   python 03_mini_gpt.py

This assembles the single attention head from step 2 into the full Transformer
architecture that GPT-style models use — just scaled way down so it trains on a
laptop CPU in a few minutes. When it finishes it SAVES the trained weights to
model.pt, and generate.py can then produce text instantly without retraining.

WHAT WE ADD ON TOP OF STEP 2:
  - MULTI-HEAD attention: run several attention heads in parallel (each can
    focus on a different kind of relationship) and combine them.
  - A FEED-FORWARD layer: after mixing information across positions with
    attention, each position "thinks" about what it gathered.
  - RESIDUAL CONNECTIONS (the `x + ...` pattern) and LAYER NORM: two tricks
    that let us stack many layers deep without training falling apart.
  - We stack N_LAYER of these "blocks" to make the model deeper = smarter.

Everything is still tiny by default (see config.py). Turn the knobs there up for
better text at the cost of a hotter, slower laptop; turn them down to go lighter.
"""

import torch
import torch.nn as nn
from torch.nn import functional as F

import config

torch.manual_seed(config.SEED)

# --- tokenizer + data: identical to steps 1 and 2 ---
with open(config.DATA_PATH, "r", encoding="utf-8") as f:
    text = f.read()
chars = sorted(list(set(text)))
vocab_size = len(chars)
stoi = {ch: i for i, ch in enumerate(chars)}
itos = {i: ch for i, ch in enumerate(chars)}
encode = lambda s: [stoi[c] for c in s]
decode = lambda ids: "".join(itos[i] for i in ids)
data = torch.tensor(encode(text), dtype=torch.long)
n = int(0.9 * len(data))
train_data, val_data = data[:n], data[n:]


def get_batch(split):
    d = train_data if split == "train" else val_data
    ix = torch.randint(len(d) - config.BLOCK_SIZE, (config.BATCH_SIZE,))
    x = torch.stack([d[i:i + config.BLOCK_SIZE] for i in ix])
    y = torch.stack([d[i + 1:i + config.BLOCK_SIZE + 1] for i in ix])
    return x.to(config.DEVICE), y.to(config.DEVICE)


@torch.no_grad()
def estimate_loss(model):
    out = {}
    model.eval()
    for split in ("train", "val"):
        losses = torch.zeros(config.EVAL_ITERS)
        for k in range(config.EVAL_ITERS):
            x, y = get_batch(split)
            _, loss = model(x, y)
            losses[k] = loss.item()
        out[split] = losses.mean().item()
    model.train()
    return out


# ===========================================================================
# BUILDING BLOCKS
# ===========================================================================
class Head(nn.Module):
    """One head of self-attention (same as step 2, see 02 for full comments)."""

    def __init__(self, head_size):
        super().__init__()
        self.key = nn.Linear(config.N_EMBD, head_size, bias=False)
        self.query = nn.Linear(config.N_EMBD, head_size, bias=False)
        self.value = nn.Linear(config.N_EMBD, head_size, bias=False)
        self.register_buffer(
            "tril", torch.tril(torch.ones(config.BLOCK_SIZE, config.BLOCK_SIZE))
        )
        self.dropout = nn.Dropout(config.DROPOUT)

    def forward(self, x):
        B, T, C = x.shape
        k = self.key(x)
        q = self.query(x)
        wei = q @ k.transpose(-2, -1) * k.shape[-1] ** -0.5
        wei = wei.masked_fill(self.tril[:T, :T] == 0, float("-inf"))
        wei = F.softmax(wei, dim=-1)
        wei = self.dropout(wei)
        v = self.value(x)
        return wei @ v


class MultiHeadAttention(nn.Module):
    """Several attention heads in parallel, their outputs concatenated.

    Each head has a smaller head_size so that all heads concatenated back to
    N_EMBD. Different heads can learn to attend to different things.
    """

    def __init__(self, num_heads, head_size):
        super().__init__()
        self.heads = nn.ModuleList([Head(head_size) for _ in range(num_heads)])
        # a linear layer to mix the concatenated head outputs back together
        self.proj = nn.Linear(head_size * num_heads, config.N_EMBD)
        self.dropout = nn.Dropout(config.DROPOUT)

    def forward(self, x):
        out = torch.cat([h(x) for h in self.heads], dim=-1)
        out = self.dropout(self.proj(out))
        return out


class FeedForward(nn.Module):
    """A little per-position MLP: after attention gathers info across positions,
    this lets each position process it. The 4x widening is the standard GPT recipe."""

    def __init__(self):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(config.N_EMBD, 4 * config.N_EMBD),
            nn.ReLU(),
            nn.Linear(4 * config.N_EMBD, config.N_EMBD),
            nn.Dropout(config.DROPOUT),
        )

    def forward(self, x):
        return self.net(x)


class Block(nn.Module):
    """One Transformer block: attention, then feed-forward.

    Note the two patterns that make deep networks trainable:
      - `x = x + ...` are RESIDUAL CONNECTIONS: the block learns an *adjustment*
        to x rather than replacing it, so gradients flow cleanly through many layers.
      - LayerNorm (`ln1`, `ln2`) keeps each position's vector well-scaled before
        each sub-layer. Applying norm BEFORE the sub-layer is the modern "pre-norm" setup.
    """

    def __init__(self, n_embd, n_head):
        super().__init__()
        head_size = n_embd // n_head
        self.sa = MultiHeadAttention(n_head, head_size)
        self.ffwd = FeedForward()
        self.ln1 = nn.LayerNorm(n_embd)
        self.ln2 = nn.LayerNorm(n_embd)

    def forward(self, x):
        x = x + self.sa(self.ln1(x))     # communicate across positions
        x = x + self.ffwd(self.ln2(x))   # then think per-position
        return x


# ===========================================================================
# THE MINI-GPT
# ===========================================================================
class MiniGPT(nn.Module):
    def __init__(self, vocab_size):
        super().__init__()
        self.token_embedding = nn.Embedding(vocab_size, config.N_EMBD)
        self.position_embedding = nn.Embedding(config.BLOCK_SIZE, config.N_EMBD)
        # the stack of Transformer blocks — this is the model's "depth"
        self.blocks = nn.Sequential(
            *[Block(config.N_EMBD, config.N_HEAD) for _ in range(config.N_LAYER)]
        )
        self.ln_f = nn.LayerNorm(config.N_EMBD)   # a final norm
        self.lm_head = nn.Linear(config.N_EMBD, vocab_size)

    def forward(self, idx, targets=None):
        B, T = idx.shape
        tok = self.token_embedding(idx)
        pos = self.position_embedding(torch.arange(T, device=config.DEVICE))
        x = tok + pos
        x = self.blocks(x)
        x = self.ln_f(x)
        logits = self.lm_head(x)

        if targets is None:
            return logits, None
        B, T, C = logits.shape
        loss = F.cross_entropy(logits.view(B * T, C), targets.view(B * T))
        return logits, loss

    @torch.no_grad()
    def generate(self, idx, max_new_tokens):
        for _ in range(max_new_tokens):
            idx_cond = idx[:, -config.BLOCK_SIZE:]   # crop to context window
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :]
            probs = F.softmax(logits, dim=-1)
            next_id = torch.multinomial(probs, num_samples=1)
            idx = torch.cat((idx, next_id), dim=1)
        return idx


def main():
    model = MiniGPT(vocab_size).to(config.DEVICE)
    n_params = sum(p.numel() for p in model.parameters())
    print(f"Model parameters: {n_params:,} (~{n_params / 1e6:.2f}M) — still tiny\n")

    optimizer = torch.optim.AdamW(model.parameters(), lr=config.LEARNING_RATE)

    print("Training the mini-GPT... watch the loss fall.")
    for step in range(config.MAX_ITERS):
        if step % config.EVAL_INTERVAL == 0 or step == config.MAX_ITERS - 1:
            losses = estimate_loss(model)
            print(f"  step {step:5d} | train loss {losses['train']:.4f} "
                  f"| val loss {losses['val']:.4f}")
        xb, yb = get_batch("train")
        _, loss = model(xb, yb)
        optimizer.zero_grad(set_to_none=True)
        loss.backward()
        optimizer.step()

    # Save everything generate.py needs to reload this exact model.
    torch.save(
        {
            "model_state": model.state_dict(),
            "stoi": stoi,
            "itos": itos,
            "vocab_size": vocab_size,
        },
        config.CHECKPOINT_PATH,
    )
    print(f"\nSaved trained model to {config.CHECKPOINT_PATH}")

    print("\n--- Generated sample (should look like Shakespeare-ish text) ---")
    start = torch.zeros((1, 1), dtype=torch.long, device=config.DEVICE)
    out = model.generate(start, max_new_tokens=config.MAX_NEW_TOKENS)
    print(decode(out[0].tolist()))
    print("\nTip: run  python generate.py  to make more text instantly (no retraining).")


if __name__ == "__main__":
    main()
