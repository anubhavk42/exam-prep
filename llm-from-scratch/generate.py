"""
generate.py — make text from the ALREADY-TRAINED model (the LIGHT step)
========================================================================

Run me with:
    python generate.py                       # 500 chars, random start
    python generate.py --chars 1000          # generate more
    python generate.py --prompt "ROMEO:"     # start from your own text

This is the part that stays light on ANY device. Training (step 3) is the heavy
bit that warms the laptop for a few minutes; once model.pt exists, generating
text is just a handful of fast forward passes — near-instant and cool. You could
run this all day on a weak phone without it heating up.

It reuses the exact MiniGPT architecture from 03_mini_gpt.py (loaded below) and
just fills in the weights we saved during training, so the two can never drift
out of sync.
"""

import argparse
import importlib.util
import os
import sys

import torch

import config


def load_minigpt_class():
    """Import the MiniGPT class from 03_mini_gpt.py.

    The filename starts with a digit, so a normal `import` won't work — we load
    it by path instead. Importing only defines the classes; it does NOT start
    training, because that lives behind 03's `if __name__ == '__main__'` guard.
    """
    spec = importlib.util.spec_from_file_location(
        "mini_gpt_module",
        os.path.join(os.path.dirname(__file__), "03_mini_gpt.py"),
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.MiniGPT


def main():
    parser = argparse.ArgumentParser(description="Generate text from the trained mini-GPT")
    parser.add_argument("--chars", type=int, default=config.MAX_NEW_TOKENS,
                        help="how many characters to generate")
    parser.add_argument("--prompt", type=str, default="",
                        help="optional text to start the generation from")
    args = parser.parse_args()

    if not os.path.exists(config.CHECKPOINT_PATH):
        sys.exit(
            f"No trained model found at '{config.CHECKPOINT_PATH}'.\n"
            f"Train one first with:  python 03_mini_gpt.py"
        )

    # Load the saved weights + the tokenizer tables we trained with.
    ckpt = torch.load(config.CHECKPOINT_PATH, map_location=config.DEVICE, weights_only=False)
    stoi, itos = ckpt["stoi"], ckpt["itos"]
    encode = lambda s: [stoi[c] for c in s]
    decode = lambda ids: "".join(itos[i] for i in ids)

    # Rebuild the model and load the trained weights into it.
    MiniGPT = load_minigpt_class()
    model = MiniGPT(ckpt["vocab_size"]).to(config.DEVICE)
    model.load_state_dict(ckpt["model_state"])
    model.eval()   # inference mode: turns off dropout

    # Build the starting context. Either the user's prompt, or a single newline.
    if args.prompt:
        # Skip any characters the model never saw during training.
        known = [c for c in args.prompt if c in stoi]
        if not known:
            print("(None of your prompt's characters are in the model's vocab; "
                  "starting from a blank line instead.)")
            start_ids = [stoi["\n"]] if "\n" in stoi else [0]
        else:
            start_ids = encode("".join(known))
    else:
        start_ids = [stoi["\n"]] if "\n" in stoi else [0]

    idx = torch.tensor([start_ids], dtype=torch.long, device=config.DEVICE)

    out = model.generate(idx, max_new_tokens=args.chars)
    print(decode(out[0].tolist()))


if __name__ == "__main__":
    main()
