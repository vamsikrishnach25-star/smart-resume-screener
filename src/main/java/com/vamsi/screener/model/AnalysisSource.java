package com.vamsi.screener.model;

/** Records which engine produced a piece of analysis, so results stay auditable. */
public enum AnalysisSource {

    /** Produced by the configured large language model. */
    LLM,

    /** Produced by the built-in deterministic engine (no API key, or the model call failed). */
    HEURISTIC
}
