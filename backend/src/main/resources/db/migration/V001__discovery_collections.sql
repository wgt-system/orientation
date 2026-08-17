CREATE TABLE discovery_collections (
    collection_id TEXT PRIMARY KEY,
    import_fingerprint TEXT NOT NULL UNIQUE,
    researched_at TEXT NOT NULL,
    question_ref TEXT NOT NULL,
    question_text TEXT NOT NULL,
    center_label TEXT NOT NULL,
    center_longitude REAL,
    center_latitude REAL,
    radius_meters INTEGER NOT NULL CHECK (radius_meters > 0),
    imported_at TEXT NOT NULL
);

CREATE TABLE discovery_criteria (
    collection_id TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    criterion_ref TEXT NOT NULL,
    description TEXT NOT NULL,
    evaluation_mode TEXT NOT NULL,
    PRIMARY KEY (collection_id, criterion_ref),
    UNIQUE (collection_id, ordinal),
    FOREIGN KEY (collection_id) REFERENCES discovery_collections(collection_id) ON DELETE CASCADE
);

CREATE TABLE research_sources (
    collection_id TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    source_ref TEXT NOT NULL,
    url TEXT NOT NULL,
    title TEXT,
    retrieved_at TEXT NOT NULL,
    PRIMARY KEY (collection_id, source_ref),
    UNIQUE (collection_id, ordinal),
    FOREIGN KEY (collection_id) REFERENCES discovery_collections(collection_id) ON DELETE CASCADE
);

CREATE TABLE discovery_candidates (
    collection_id TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    candidate_ref TEXT NOT NULL,
    display_name TEXT NOT NULL,
    canonical_uri TEXT,
    researched_location_label TEXT NOT NULL,
    researched_longitude REAL,
    researched_latitude REAL,
    PRIMARY KEY (collection_id, candidate_ref),
    UNIQUE (collection_id, ordinal),
    FOREIGN KEY (collection_id) REFERENCES discovery_collections(collection_id) ON DELETE CASCADE
);

CREATE TABLE candidate_external_ids (
    collection_id TEXT NOT NULL,
    candidate_ref TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    provider TEXT NOT NULL,
    external_id TEXT NOT NULL,
    PRIMARY KEY (collection_id, candidate_ref, ordinal),
    FOREIGN KEY (collection_id, candidate_ref)
        REFERENCES discovery_candidates(collection_id, candidate_ref) ON DELETE CASCADE
);

CREATE TABLE candidate_location_sources (
    collection_id TEXT NOT NULL,
    candidate_ref TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    source_ref TEXT NOT NULL,
    PRIMARY KEY (collection_id, candidate_ref, ordinal),
    FOREIGN KEY (collection_id, candidate_ref)
        REFERENCES discovery_candidates(collection_id, candidate_ref) ON DELETE CASCADE,
    FOREIGN KEY (collection_id, source_ref)
        REFERENCES research_sources(collection_id, source_ref) ON DELETE RESTRICT
);

CREATE TABLE candidate_claims (
    collection_id TEXT NOT NULL,
    candidate_ref TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    criterion_ref TEXT NOT NULL,
    status TEXT NOT NULL,
    basis TEXT NOT NULL,
    observed_value_kind TEXT,
    observed_value TEXT,
    note TEXT,
    PRIMARY KEY (collection_id, candidate_ref, criterion_ref),
    UNIQUE (collection_id, candidate_ref, ordinal),
    FOREIGN KEY (collection_id, candidate_ref)
        REFERENCES discovery_candidates(collection_id, candidate_ref) ON DELETE CASCADE,
    FOREIGN KEY (collection_id, criterion_ref)
        REFERENCES discovery_criteria(collection_id, criterion_ref) ON DELETE RESTRICT
);

CREATE TABLE claim_sources (
    collection_id TEXT NOT NULL,
    candidate_ref TEXT NOT NULL,
    criterion_ref TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    source_ref TEXT NOT NULL,
    PRIMARY KEY (collection_id, candidate_ref, criterion_ref, ordinal),
    FOREIGN KEY (collection_id, candidate_ref, criterion_ref)
        REFERENCES candidate_claims(collection_id, candidate_ref, criterion_ref) ON DELETE CASCADE,
    FOREIGN KEY (collection_id, source_ref)
        REFERENCES research_sources(collection_id, source_ref) ON DELETE RESTRICT
);
