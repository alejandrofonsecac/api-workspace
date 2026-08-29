CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    description TEXT,
    location VARCHAR(255),
    source VARCHAR(20) NOT NULL CHECK (source IN ('internal', 'google')),
    category VARCHAR(20) NOT NULL CHECK (category IN ('work', 'study', 'personal', 'health', 'event', 'habit')),
    CONSTRAINT chk_events_end_time_after_start_time CHECK (end_time > start_time)
);

CREATE INDEX idx_events_date_start_time ON events (date, start_time);
