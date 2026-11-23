# Choosing a local model for the guide's small talk

The optional small talk (see the README) sends short requests to a local language model through llama.cpp's
`llama-server`. The model runs on the same computer as Minecraft, so it has to leave enough memory for the game
and still be fun to talk to. This document collects what we found (November 2026) and how we got to the
recommendation in the README. Numbers come from the sources listed at the end; where sources disagree we give a range.

## 1. The memory budget

**Minecraft: Java Edition** (requirements raised in July 2026, the first change since launch):
- minimum: a Vulkan 1.3 GPU with **2 GB VRAM**, and 8 GB RAM (12 GB with integrated graphics);
- recommended: **6 GB VRAM** (RTX 2060 class) and 16 GB RAM;
- the game itself uses about 4 GB of RAM according to Mojang.
- Shaders, high render distances and resource packs need more; the numbers above are for vanilla settings.

**What the small talk needs from a model**
- Short answers: at most `chatMaxTokens` = 80 tokens, cut to two sentences.
- A small context: a system prompt of roughly 400 tokens plus at most 6 exchanges of history. `-c 4096` is plenty;
  `-c 2048` also works and saves a little memory for the KV cache (which grows with the context size).
- Speed matters less than for a chat app: a remark that arrives a few seconds late is fine
  (`chatTimeoutSeconds` = 20).
- It must speak the player's language well (the mod asks for the game language, e.g. Dutch).
- It must **not** "think" before answering: that is slow and the thoughts are useless here. The mod already sends
  `chat_template_kwargs: {"enable_thinking": false}` with every request.
- It should be playful. The mod uses temperature 0.8 by default.

So the model should take **about 3–5 GB**, either in VRAM next to the game or in system RAM.

## 2. Candidates

All of these are instruction-tuned, run in llama.cpp, and have GGUF downloads on Hugging Face.

| Model | Size Q4 (GGUF) | Languages | License | Fit for small talk |
|---|---|---|---|---|
| **Gemma 4 E4B** (Google, April 2026) | Q4_0 4.5 GB (Google); Q4_K_M 5.0–5.3 GB (community builds) | 140+ | Apache 2.0 | **Most creative of the small models**: one of the smallest models with an EQ-Bench Creative Writing v3 score (Elo about 1093). Thinking is off by default in its chat template. |
| **Gemma 4 E2B** | Q4_0 2.9 GB (Google); Q4_K_M 3.1–3.4 GB | 140+ | Apache 2.0 | Lighter Gemma 4; fits on 8 GB cards next to the game. |
| **Qwen3.5 4B** (Alibaba, March 2026) | Q4_K_M 2.8–3.0 GB | 201 | Apache 2.0 | **Most recommended small all-rounder** in 2026 guides; strong benchmarks for its size. Recommended non-thinking sampling: temperature 0.7, top_p 0.8, top_k 20, presence penalty 1.5. |
| **Qwen3.5 2B** | about half of the 4B | 201 | Apache 2.0 | For small GPUs; expect simpler, plainer answers. |
| Phi-4-mini (3.8B) | about 2.5 GB | multilingual | MIT | Lightest footprint and good at following instructions, but more businesslike in tone. |
| Ministral 3 3B Instruct | about 2 GB | multilingual | Apache 2.0 | Mistral recommends a very low temperature (0.1–0.15) for its Instruct models: predictable, less playful. |
| Llama 3.2 3B | about 2 GB | 8 official languages | Llama license | Older (2024); the 2026 models above are stronger, especially outside English. |

**Notes on the two front runners**
- *Gemma 4 E4B* counts about 8B parameters including its per-layer embeddings, but only about 4B are "effective"
  per token. One user measured only about 2.2 GiB of the E4B Q4_0 file actually landing in VRAM; we could not
  verify that, so plan for the full file size.
- *Qwen3.5* small models are multimodal: their repos also contain a vision projector (`mmproj`). You don't need
  it for this mod; download only the main `.gguf`.
- Both chat templates want user and assistant turns to alternate; the mod already merges repeated turns.

**Dutch (and other non-English languages).** We found no Dutch benchmark that includes the small Gemma 4 and
Qwen3.5 models yet: EuroEval (formerly ScandEval) has them queued, and the Dutch word-puzzle benchmark TSW-NL
only covers older Gemma 3 and Qwen3 models. Both families claim broad multilingual training. Our advice: try
both and keep the one whose Dutch you like best.

## 3. What to avoid
- **Reasoning/thinking variants** (for example "Ministral 3 3B Reasoning"): they think out loud before
  answering. Slow, and useless for a one-line remark.
- **Very small models (under about 2B)**: our expectation (not measured) is shaky grammar outside English and
  repetitive remarks.
- **Larger models (8B and up) on the GPU** unless you have 16 GB VRAM or more: they compete with the game for
  memory and can cause stutter when VRAM runs out.
- "Uncensored" or roleplay merges: not needed for friendly small talk, and they follow the mod's rules
  (no recipes, short replies) less reliably.

## 4. Recommendation

| Your graphics card | Model | Where it runs | Memory left for the game |
|---|---|---|---|
| **12 GB VRAM or more** | Gemma 4 E4B, Q4_K_M | GPU (`-ngl 99`) | about 6 GB |
| **8 GB VRAM** | Qwen3.5 4B, Q4_K_M (or Gemma 4 E2B) | GPU (`-ngl 99`) | about 4–5 GB |
| **6 GB VRAM or less, or integrated graphics** | Qwen3.5 4B, Q4_K_M | CPU (`-ngl 0`), needs 16 GB+ RAM | all VRAM stays free |
| same, but a slow or older CPU | Qwen3.5 2B, Q4_K_M | GPU or CPU | about 1.5 GB used |

Running on the CPU is a real option here: the replies are only 80 tokens long, so a modern 6–8 core CPU
answers within a few seconds, and the game keeps all of its VRAM.

Starting the server (replace the file name with the model you downloaded):
```
llama-server -m gemma-4-E4B-it-Q4_K_M.gguf -ngl 99 -c 4096 --port 8080
llama-server -m Qwen3.5-4B-Q4_K_M.gguf -ngl 0 -c 2048 --port 8080   # CPU only
```

Tips:
- Watch the memory with `nvidia-smi` (NVIDIA) or Task Manager while playing. If the game stutters, lower `-ngl`
  (moves part of the model to the CPU) or `-c`.
- If answers arrive too late on the CPU, raise `chatTimeoutSeconds` or lower `chatMaxTokens`.
- If the guide sounds too wild or too dull, change `chatTemperature` (0.6 calm – 1.0 playful).

## 5. What we could not verify
- Dutch-language quality of the small 2026 models (no published benchmark yet).
- Exact VRAM use: GGUF file sizes differ between community builds, and the reported 2.2 GiB for Gemma 4 E4B is a
  single, unverified measurement.
- Release dates: one source puts Gemma 4 in July 2026, Wikipedia says April 2, 2026.
- Whether `enable_thinking: false` works for Qwen3.5 in every llama.cpp build: one model card says yes, another
  suggests stripping `<think>` blocks instead. The mod does both (it sends the flag and removes `<think>` blocks
  from replies).

## Sources
- Minecraft requirements: [PC Gamer](https://pcgamer.com/hardware/minecraft-java-edition-system-requirements-want-8-16-gb-ram-for-the-first-time-but-it-isnt-more-demanding-now-microsoft-just-hadnt-updated-the-specs-in-a-while), [Notebookcheck](https://www.notebookcheck.net/Mojang-raises-Minecraft-Java-Edition-s-system-requirements-for-the-first-time.1352555.0.html), [Windows Report](https://windowsreport.com/minecraft-java-edition-raises-ram-requirement-to-16gb/), [Hardware Busters](https://hwbusters.com/news/minecraft-raised-its-system-requirements-after-17-years-and-vulkan-1-3-is-the-real-cutoff/)
- Gemma 4: [Google Gemma 4 model overview](https://ai.google.dev/gemma/docs/core), [Google: thinking mode](https://ai.google.dev/gemma/docs/capabilities/thinking), [Unsloth: Gemma 4](https://unsloth.ai/docs/models/gemma-4), [Gemma 4 chat template](https://huggingface.co/google/gemma-4-E4B-it/blob/main/chat_template.jinja), [Gemma 4 download guide](https://gemma4all.com/blog/gemma-4-download), [Gemma 4 variants and VRAM](https://www.haotianblog.com/en/gemma-4-variants-performance-efficiency-en/), [Wikipedia: Gemma](https://en.wikipedia.org/wiki/Gemma_(language_model))
- Qwen3.5: [Qwen3.5-2B model card](https://huggingface.co/Qwen/Qwen3.5-2B), [Qwen3.5 small series overview](https://note.com/zephel01/n/n6b236da76680?hl=en), [Qwen3.5-4B GGUF (SoAIHQ)](https://huggingface.co/SoAIHQ/Qwen3.5-4B-GGUF), [Qwen3.5-4B GGUF (bartowski)](https://huggingface.co/bartowski/Qwen_Qwen3.5-4B-GGUF), [Qwen3.5-4B Instruct GGUF (Hob-forge)](https://huggingface.co/Hob-forge/Qwen3.5-4B-Instruct-GGUF)
- Comparisons of small models: [promptquorum: best small LLM 2026](https://www.promptquorum.com/local-llms/small-local-llm-models), [Popular AI: CPU-only LLMs 2026](https://www.popularai.org/p/best-cpu-only-local-llm-2026), [AI Tool Radar: best local LLM](https://aitoolradar.io/guides/best-local-llm), [Gemma 4 vs Qwen 3.5](https://codersera.com/blog/gemma-4-vs-qwen-3-5-comparison-2026/), [vdf.ai: best local LLM for writing](https://vdf.ai/blog/best-local-llm-for-writing/)
- Ministral 3: [Unsloth: Ministral 3](https://unsloth.ai/docs/models/tutorials/ministral-3), [Mistral: Ministral 3 checkpoints](https://huggingface.co/collections/mistralai/ministral-3-additional-checkpoints)
- Dutch evaluation: [EuroEval core models issue](https://github.com/EuroEval/EuroEval/issues/1186), [The Shared Word (TSW-NL)](https://arxiv.org/html/2611.22935), [DataNorth: best AI model for Dutch](https://datanorth.ai/blog/which-ai-models-are-actually-good-at-dutch)
- llama.cpp memory: [llama.cpp n-gpu-layers VRAM guide](https://dev.to/pat9000/how-to-tune-llamacpp-n-gpu-layers-a-practical-vram-guide-2026-m8i), [Why does llama.cpp use so much VRAM? (discussion)](https://github.com/ggml-org/llama.cpp/discussions/9784)
