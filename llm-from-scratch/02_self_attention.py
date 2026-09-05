"""
STEP 2 of 3 — ADDING SELF-ATTENTION (the idea that makes Transformers work)
============================================================================

Run me with:   python 02_self_attention.py

WHAT CHANGED FROM STEP 1: the bigram model could only see the ONE current
character. Here we add a single "self-attention head" so each position can look
BACK at the characters before it and mix in information from them. This is THE
core idea behind every modern LLM (GPT, Claude, etc.) — the "attention" in
"Attention Is All You Need". The output here is still far from perfect, but you
should see it become noticeably more word-like than step 1's pure gibberish.

THE INTUITION (read this once, slowly):
  When predicting the next character, not every earlier character matters
  equally. After "To be, or not to b" the model should pay a LOT of attention
  to the recent letters to guess "e". Self-attention lets each position decide,
  for itself, how much to focus on each earlier position, and then pull in a
  blend of their information. It does this with three vectors per position:
    - QUERY: "what am I looking for?"
    - KEY:   "what do I contain?"
    - VALUE: "what will I pass on if you attend to me?"
  A position's query is compared against every earlier position's key. High
  match = high attention weight. The output is the attention-weighted sum of
  the values. That's it.

We now also add POSITION embeddings, because attention on its own has no sense
of order — we have to tell it where each character sits in the sequence.
"""

import torch
import torch.nn as nn
from torch.nn import functional as F

import config

torch.manual_seed(config.SEED)

# --- tokenizer + data: identical to step 1 (see 01_bigram.py for comments) ---
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
# THE NEW PART: a single self-attention head
# ===========================================================================
class Head(nn.Module):
    """One head of self-attention. This is the heart of the whole project."""

    def __init__(self, head_size):
        super().__init__()
        # Three linear layers turn each position's vector into its query, key,
        # and value. They have no bias — just learned projections.
        self.key = nn.Linear(config.N_EMBD, head_size, bias=False)
        self.query = nn.Linear(config.N_EMBD, head_size, bias=False)
        self.value = nn.Linear(config.N_EMBD, head_size, bias=False)

        # A lower-triangular matrix of 1s used as a MASK. It's what makes this
        # "causal": position t may attend to positions 0..t but NEVER to the
        # future (that would be cheating — peeking at the answer). Registered
        # as a buffer so it moves with the model but isn't a learned weight.
        self.register_buffer(
            "tril", torch.tril(torch.ones(config.BLOCK_SIZE, config.BLOCK_SIZE))
        )
        self.dropout = nn.Dropout(config.DROPOUT)

    def forward(self, x):
        B, T, C = x.shape           # batch, time (positions), channels
        k = self.key(x)             # (B, T, head_size)
        q = self.query(x)           # (B, T, head_size)

        # 1. SCORES: every query dotted with every key = how much each position
        #    should attend to each other position. Scale by sqrt(head_size) to
        #    keep the numbers in a sane range (the "scaled" in scaled dot-product).
        wei = q @ k.transpose(-2, -1) * k.shape[-1] ** -0.5   # (B, T, T)

        # 2. MASK: blank out the future by setting it to -infinity, so that
        #    after softmax those positions get exactly zero attention.
        wei = wei.masked_fill(self.tril[:T, :T] == 0, float("-inf"))

        # 3. SOFTMAX: turn the scores into weights that sum to 1 across each row.
        wei = F.softmax(wei, dim=-1)
        wei = self.dropout(wei)

        # 4. GATHER VALUES: the output is the attention-weighted sum of values.
        v = self.value(x)           # (B, T, head_size)
        out = wei @ v               # (B, T, head_size)
        return out


class SingleHeadModel(nn.Module):
    """Bigram idea + ONE attention head so positions can see their past."""

    def __init__(self, vocab_size):
        super().__init__()
        # token embedding: character id -> a vector of width N_EMBD
        self.token_embedding = nn.Embedding(vocab_size, config.N_EMBD)
        # position embedding: position 0..BLOCK_SIZE-1 -> a vector (gives order)
        self.position_embedding = nn.Embedding(config.BLOCK_SIZE, config.N_EMBD)
        # our single attention head (head_size == N_EMBD here for simplicity)
        self.sa_head = Head(config.N_EMBD)
        # final linear layer maps the vector back to a score per vocab character
        self.lm_head = nn.Linear(config.N_EMBD, vocab_size)

    def forward(self, idx, targets=None):
        B, T = idx.shape
        tok = self.token_embedding(idx)                                  # (B,T,C)
        pos = self.position_embedding(torch.arange(T, device=config.DEVICE))  # (T,C)
        x = tok + pos          # add position info to token info
        x = self.sa_head(x)    # <-- positions now mix in info from their past
        logits = self.lm_head(x)                                         # (B,T,vocab)

        if targets is None:
            return logits, None
        B, T, C = logits.shape
        loss = F.cross_entropy(logits.view(B * T, C), targets.view(B * T))
        return logits, loss

    @torch.no_grad()
    def generate(self, idx, max_new_tokens):
        for _ in range(max_new_tokens):
            # attention can only see BLOCK_SIZE positions, so crop the context
            idx_cond = idx[:, -config.BLOCK_SIZE:]
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :]
            probs = F.softmax(logits, dim=-1)
            next_id = torch.multinomial(probs, num_samples=1)
            idx = torch.cat((idx, next_id), dim=1)
        return idx


def main():
    model = SingleHeadModel(vocab_size).to(config.DEVICE)
    n_params = sum(p.numel() for p in model.parameters())
    print(f"Model parameters: {n_params:,}\n")

    optimizer = torch.optim.AdamW(model.parameters(), lr=config.LEARNING_RATE)

    print("Training... (val loss should end up LOWER than step 1's bigram)")
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

    print("\n--- Generated sample (more word-like than step 1) ---")
    start = torch.zeros((1, 1), dtype=torch.long, device=config.DEVICE)
    out = model.generate(start, max_new_tokens=config.MAX_NEW_TOKENS)
    print(decode(out[0].tolist()))


if __name__ == "__main__":
    main()
