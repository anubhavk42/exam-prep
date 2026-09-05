"""
STEP 1 of 3 — THE BIGRAM MODEL (the simplest possible "language model")
========================================================================

Run me with:   python 01_bigram.py

WHAT YOU'LL SEE: the loss number goes down as it trains, then it prints some
generated text that is basically GIBBERISH. That is the whole point of this
step — it's the "before" picture. It's the dumbest model that still counts as a
language model, and everything in steps 2 and 3 is about making it smarter.

WHAT A "BIGRAM" MODEL IS: it predicts the next character using ONLY the single
current character. "bi-gram" = pairs of two. Given the letter 'q' it learns
that 'u' is likely next; given a space it learns a capital letter is likely.
It has NO memory beyond the one character it's looking at right now. That's why
it can spell tiny fragments but can't form real words or sentences.

This file introduces the four things EVERY language model needs, and steps 2/3
reuse all of them:
  1. a tokenizer      (turn text into numbers and back)
  2. batching         (feed the model many little chunks of text at once)
  3. a training loop  (predict -> measure error -> nudge weights -> repeat)
  4. generation       (feed the model its own output to make it write)
"""

import torch
import torch.nn as nn
from torch.nn import functional as F

import config

torch.manual_seed(config.SEED)


# ===========================================================================
# 1. THE TOKENIZER  —  turning text into numbers
# ===========================================================================
# A neural network only understands numbers, not letters. So the very first
# job is to map every character to an integer id. We use a CHARACTER-LEVEL
# tokenizer: every single character (letters, space, newline, punctuation) is
# one token. This keeps the vocabulary tiny (a few dozen symbols) which is
# exactly why it stays light — no giant word list, no downloads.

with open(config.DATA_PATH, "r", encoding="utf-8") as f:
    text = f.read()

# `chars` is the sorted list of every unique character in our text = the vocab.
chars = sorted(list(set(text)))
vocab_size = len(chars)

# Two lookup tables: character -> id, and id -> character.
stoi = {ch: i for i, ch in enumerate(chars)}   # "string to integer"
itos = {i: ch for i, ch in enumerate(chars)}   # "integer to string"

# encode: a string  -> a list of integer ids
# decode: a list of ids -> a string
encode = lambda s: [stoi[c] for c in s]
decode = lambda ids: "".join(itos[i] for i in ids)

print(f"Text length: {len(text)} characters")
print(f"Vocabulary size: {vocab_size} unique characters")
print(f"The vocabulary: {''.join(chars)!r}\n")

# Turn the ENTIRE text into one long tensor of ids. This is our dataset.
data = torch.tensor(encode(text), dtype=torch.long)

# Split 90% for training, 10% held back to check we're not just memorising.
n = int(0.9 * len(data))
train_data = data[:n]
val_data = data[n:]


# ===========================================================================
# 2. BATCHING  —  grabbing random chunks of text to learn from
# ===========================================================================
def get_batch(split):
    """Return a small batch of (input, target) pairs.

    For each chunk of `block_size` characters (the input `x`), the target `y`
    is the SAME chunk shifted one character to the right — because the model's
    job is "given these characters, predict the next one" at every position.
    """
    d = train_data if split == "train" else val_data
    # pick BATCH_SIZE random starting points in the text
    ix = torch.randint(len(d) - config.BLOCK_SIZE, (config.BATCH_SIZE,))
    x = torch.stack([d[i:i + config.BLOCK_SIZE] for i in ix])
    y = torch.stack([d[i + 1:i + config.BLOCK_SIZE + 1] for i in ix])
    return x.to(config.DEVICE), y.to(config.DEVICE)


@torch.no_grad()  # we're only measuring, not learning, so skip gradient tracking
def estimate_loss(model):
    """Average the loss over several batches for a steadier number to print."""
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
# 3. THE MODEL  —  a bigram is literally just one lookup table
# ===========================================================================
class BigramLanguageModel(nn.Module):
    def __init__(self, vocab_size):
        super().__init__()
        # This single table IS the entire model. Row `i` holds the model's
        # predicted scores ("logits") for what character comes after character
        # `i`. Shape: (vocab_size, vocab_size). During training these numbers
        # get nudged so the right next-character scores highest.
        self.token_table = nn.Embedding(vocab_size, vocab_size)

    def forward(self, idx, targets=None):
        # idx has shape (B, T): B sequences, each T characters long.
        # Looking each id up in the table gives scores for the next char.
        logits = self.token_table(idx)          # (B, T, vocab_size)

        if targets is None:
            return logits, None

        # Cross-entropy compares the predicted scores against the true next
        # character and produces a single "how wrong are we" number = the loss.
        # PyTorch wants the shape flattened to (B*T, vocab_size) for this.
        B, T, C = logits.shape
        loss = F.cross_entropy(logits.view(B * T, C), targets.view(B * T))
        return logits, loss

    @torch.no_grad()
    def generate(self, idx, max_new_tokens):
        """Write text one character at a time, feeding output back as input."""
        for _ in range(max_new_tokens):
            logits, _ = self(idx)          # get predictions
            logits = logits[:, -1, :]      # we only care about the LAST position
            probs = F.softmax(logits, dim=-1)   # turn scores into probabilities
            # Sample the next character (random pick weighted by probability —
            # this is why the output differs each run, and why it's creative).
            next_id = torch.multinomial(probs, num_samples=1)
            idx = torch.cat((idx, next_id), dim=1)   # append it and loop
        return idx


# ===========================================================================
# 4. TRAIN, THEN GENERATE
# ===========================================================================
def main():
    model = BigramLanguageModel(vocab_size).to(config.DEVICE)
    n_params = sum(p.numel() for p in model.parameters())
    print(f"Model parameters: {n_params:,} (tiny!)\n")

    # The optimizer is what actually adjusts the weights to reduce the loss.
    optimizer = torch.optim.AdamW(model.parameters(), lr=config.LEARNING_RATE)

    print("Training... (loss should fall over time)")
    for step in range(config.MAX_ITERS):
        if step % config.EVAL_INTERVAL == 0 or step == config.MAX_ITERS - 1:
            losses = estimate_loss(model)
            print(f"  step {step:5d} | train loss {losses['train']:.4f} "
                  f"| val loss {losses['val']:.4f}")

        # --- the four lines that ARE "learning" ---
        xb, yb = get_batch("train")          # 1. get a batch
        _, loss = model(xb, yb)              # 2. predict + measure error
        optimizer.zero_grad(set_to_none=True)
        loss.backward()                      # 3. work out which way to nudge
        optimizer.step()                     # 4. take the nudge

    # Generate a sample. We start from a single newline character as a "seed".
    print("\n--- Generated sample (expect gibberish — that's correct!) ---")
    start = torch.zeros((1, 1), dtype=torch.long, device=config.DEVICE)
    out = model.generate(start, max_new_tokens=config.MAX_NEW_TOKENS)
    print(decode(out[0].tolist()))


if __name__ == "__main__":
    main()
