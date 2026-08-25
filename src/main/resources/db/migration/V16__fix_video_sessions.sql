DROP TABLE IF EXISTS consultation_video_sessions;

CREATE TABLE consultation_video_sessions (
    id UUID PRIMARY KEY,
    consultation_id UUID UNIQUE NOT NULL REFERENCES consultation_sessions(id),
    provider VARCHAR(50) NOT NULL DEFAULT 'LIVEKIT',
    provider_room_name VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    started_by_user_id UUID NOT NULL REFERENCES app_users(id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_consultation_video_sessions_consultation_id ON consultation_video_sessions(consultation_id);
