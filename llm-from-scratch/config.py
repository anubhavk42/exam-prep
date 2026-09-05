"""
config.py — every "knob" for the mini-GPT lives here, in ONE place.

This is where we keep things LIGHT. The whole philosophy of this project is
"small numbers everywhere" so it trains in a few minutes on a plain laptop CPU
and never overheats. Each setting below has a comment telling you which way to
turn it to go *lighter* (faster, cooler, dumber) or *heavier* (slower, hotter,
smarter).

All three training scripts (01, 02, 03) and generate.py import from here, so if
you change a number here it changes everywhere consistently.
"""

import torch

# ---------------------------------------------------------------------------
# WHERE THE MODEL RUNS
# ---------------------------------------------------------------------------
# We default to the CPU on purpose — it's what every laptop and phone has, and
# it keeps this project runnable anywhere. If you happen to have a GPU, this
# will quietly use it and go faster, but it is NEVER required.
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# A fixed random seed so your runs are repeatable (you get the same "random"
# numbers each time). Change it to see different results.
SEED = 1337

# ---------------------------------------------------------------------------
# THE DATA
# ---------------------------------------------------------------------------
# Path to the plain-text file the model learns to imitate. Swap this file for
# your own .txt (song lyrics, your notes, a book) to train on something else.
DATA_PATH = "data/input.txt"

# ---------------------------------------------------------------------------
# MODEL SIZE  (these decide how "big" the brain is — and how hot the laptop gets)
# ---------------------------------------------------------------------------
# block_size = context length = how many characters back the model can "see"
# when predicting the next one. Bigger = more context but more compute.
#   lighter: 32     heavier: 256
BLOCK_SIZE = 64

# n_embd = the width of the model (size of each token's vector). This is the
# single biggest lever on size/speed. Bigger = smarter but much heavier.
#   lighter: 32     heavier: 256
N_EMBD = 96

# n_head = how many attention "heads" run in parallel. N_EMBD must divide evenly
# by n_head (96 / 4 = 24, fine). More heads = more ways to look at context.
#   lighter: 2      heavier: 8
N_HEAD = 4

# n_layer = how many transformer blocks are stacked. Depth. More = smarter/slower.
#   lighter: 2      heavier: 8
N_LAYER = 4

# dropout = randomly "turns off" some connections during training to reduce
# overfitting. On a tiny dataset a little helps. 0.0 turns it off entirely.
DROPOUT = 0.1

# ---------------------------------------------------------------------------
# TRAINING  (how long we learn for — the ONLY genuinely "heavy" phase)
# ---------------------------------------------------------------------------
# batch_size = how many text chunks we learn from at once. Bigger = smoother
# learning but more RAM/CPU per step.
#   lighter: 16     heavier: 64
BATCH_SIZE = 32

# max_iters = how many training steps in total. This is the main "how long does
# it run / how hot does it get" dial. Fewer = finishes sooner, learns less.
# On the small bundled sample this is deliberately modest to stay light and cool
# (~2-3 min on a laptop CPU) — the model overfits tiny text quickly, so piling on
# more steps just memorises it. If you train on a LARGER .txt, raise this.
#   quick test: 300     bundled sample: ~1200     large text: 3000-5000+
MAX_ITERS = 1200

# How often to pause and print the current loss (lower loss = learning working).
EVAL_INTERVAL = 200

# How many batches to average when measuring loss (bigger = steadier number).
EVAL_ITERS = 50

# learning_rate = how big a step the optimizer takes each update. Tiny models
# tolerate a fairly high rate. Too high = unstable, too low = slow.
LEARNING_RATE = 3e-3

# ---------------------------------------------------------------------------
# GENERATION
# ---------------------------------------------------------------------------
# Default number of characters to produce when sampling text.
MAX_NEW_TOKENS = 500

# Where step 3 saves the trained weights, and where generate.py loads them from.
CHECKPOINT_PATH = "model.pt"
