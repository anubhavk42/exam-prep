# Build an LLM from Scratch — a tiny, laptop-friendly mini-GPT

A hands-on, **educational** build of a language model from the ground up, in
Python + PyTorch. It's deliberately **tiny and CPU-only** so it trains in a few
minutes on a modest laptop and never cooks your device — then runs cool on
almost anything, including a phone.

You write and read every core piece yourself: the tokenizer, self-attention, the
Transformer blocks, the training loop, and text generation. It's the same
architecture family as GPT and Claude — just scaled down by a factor of millions.

> **Honest expectations:** this learns the *style and structure* of whatever text
> you train it on and produces plausible-looking imitation of it. It does **not**
> answer questions or chat like ChatGPT/Claude — that takes enormous data,
> compute, and extra training stages. What you get here is the real *engine*, in
> miniature, so you understand how the big ones actually work.

## Why it stays light (the key idea)

**Training is the only heavy step; running the trained model is light.**

| | Training (`03_mini_gpt.py`) | Running (`generate.py`) |
|---|---|---|
| What it does | Model learns from the text | Model just writes text |
| Load on device | A few minutes of CPU, warms up mildly | Near-instant, stays cool |

So we keep the model tiny, train it once, and after that generating text is
cheap. Every "size" knob lives in [`config.py`](config.py) with a comment on
which way makes it lighter or heavier.

## Setup

Requires Python 3.9+.

```sh
cd llm-from-scratch
pip install -r requirements.txt --index-url https://download.pytorch.org/whl/cpu
```

The `--index-url` grabs the smaller **CPU-only** PyTorch build (no GPU needed).
If that URL is unavailable in your environment, plain `pip install -r requirements.txt`
also works — it just downloads a larger wheel that still runs fine on CPU.

## Run it in order (this is the whole lesson)

Each script is standalone and heavily commented. Run them in sequence and watch
the model get smarter at each step — the **loss** number falling means it's
learning, and the generated sample gets more text-like.

```sh
python 01_bigram.py          # Step 1: simplest model. Output = gibberish (on purpose)
python 02_self_attention.py  # Step 2: add attention. Output gets more word-like
python 03_mini_gpt.py        # Step 3: full mini-GPT. Trains + saves model.pt
python generate.py           # Make text instantly from the saved model
```

### What each file teaches

| File | Idea it introduces |
|---|---|
| [`config.py`](config.py) | Every hyperparameter ("small numbers everywhere"), in one place |
| [`01_bigram.py`](01_bigram.py) | Tokenizer, batching, training loop, generation — the baseline with **no memory** |
| [`02_self_attention.py`](02_self_attention.py) | **Self-attention**: query/key/value, causal mask, softmax — the core of every LLM |
| [`03_mini_gpt.py`](03_mini_gpt.py) | Multi-head attention, feed-forward, residuals + LayerNorm, stacked blocks = a real Transformer |
| [`generate.py`](generate.py) | Loading trained weights and sampling text — the light inference step |

### Roughly what you'll see

Each step is smarter than the last (lower loss = better). On the tiny bundled
sample:

```
Step 1 (bigram):        val loss ~3.5   → gibberish
Step 2 (attention):     val loss ~2.5   → word-shaped fragments
Step 3 (mini-GPT):      val loss ~2.3   → character names, line breaks, Shakespeare-ish text
```

Step 3 trains in roughly **2–3 minutes** on a laptop CPU with the default
settings — light enough that the machine barely warms up.

> **You'll notice the val loss stops falling and starts *rising* partway through
> step 3.** That's not a bug — it's **overfitting**, and it's expected here: the
> bundled text is only a few KB, so the model quickly starts *memorising* it
> instead of learning general patterns. The generated text still looks great
> (it's recombining lines it learned). The fix for real generalisation is **more
> training text**, not more steps — see below.

`generate.py` examples:

```sh
python generate.py                     # 500 characters, random start
python generate.py --chars 1000        # generate more
python generate.py --prompt "ROMEO:"   # start from your own text
```

## Train on your own text

Replace [`data/input.txt`](data/input.txt) with any plain-text `.txt` file — song
lyrics, your own notes, a novel, code — then run `python 03_mini_gpt.py` again.
The character-level tokenizer adapts automatically to whatever characters appear.
**More text = better results**; the bundled sample is small (a few KB) so it runs
fast, which means the model overfits (partly memorises) it. Give it more text
(hundreds of KB to a few MB) for genuinely novel output.

## Make it lighter or heavier

All in [`config.py`](config.py):

- **Lighter / faster / cooler** (great for a weak laptop or phone): lower
  `N_EMBD`, `N_LAYER`, `BLOCK_SIZE`, and `MAX_ITERS`.
- **Smarter / slower / warmer**: raise them, and give it more training text.
- `MAX_ITERS` is the main "how long does it run" dial. Start with `500` for a
  quick test; `3000`–`5000` gives a nicer result on the bundled text.

## What this is *not*

- Not a chatbot or assistant — it imitates text, it doesn't understand requests.
- Not using any pre-trained weights or internet data — it learns only from your
  `input.txt`, entirely offline.
- Not the fastest possible implementation — it's optimised for **readability and
  learning**, not speed.

## Where to go next

- **Run it inside an app / on a phone natively:** export the trained model to
  TorchScript or ONNX for mobile runtimes. (Follow-up project.)
- **Word/subword tokenizer** instead of character-level (e.g. BPE) — how real
  LLMs handle large vocabularies.
- **More data + bigger config** — watch quality climb as you scale, which is
  exactly the story of how the frontier models got so capable.

## Credits

The step-by-step character-level approach here follows the well-known teaching
path popularised by Andrej Karpathy's "Let's build GPT from scratch" and nanoGPT.
Reimplemented and commented from scratch for learning. The bundled sample text is
public-domain Shakespeare.
