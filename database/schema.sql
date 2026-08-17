PRAGMA foreign_keys = ON;

-- =========================================================
-- Controllers
-- =========================================================

CREATE TABLE Controllers (
    controller_id TEXT PRIMARY KEY NOT NULL,
    label TEXT NOT NULL
);


-- =========================================================
-- Devices
-- =========================================================

CREATE TABLE Devices (
    device_id TEXT PRIMARY KEY NOT NULL,
    controller_id TEXT,
    label TEXT NOT NULL,

    FOREIGN KEY (controller_id)
        REFERENCES Controllers(controller_id)
        ON DELETE SET NULL
);


-- =========================================================
-- Device Capabilities
-- =========================================================

CREATE TABLE DeviceCapabilities (
    device_id TEXT NOT NULL,
    capability_key TEXT NOT NULL,
    label TEXT NOT NULL,

    PRIMARY KEY (device_id, capability_key),

    FOREIGN KEY (device_id)
        REFERENCES Devices(device_id)
        ON DELETE CASCADE
);


-- =========================================================
-- Parameter Requirements
--
-- Corresponds to ParameterRequirement.oneof requirement.
-- Only the columns relevant to requirement_type are populated.
-- =========================================================

CREATE TABLE ParameterRequirements (
    device_id TEXT NOT NULL,
    capability_key TEXT NOT NULL,

    parameter_key TEXT NOT NULL,
    label TEXT NOT NULL,

    requirement_type TEXT NOT NULL
        CHECK (
            requirement_type IN (
                'STRING',
                'UINT32',
                'INT32',
                'BOOL',
                'RGBA',
                'PERCENTAGE',
                'FILE',
                'DOUBLE'
            )
        ),

    -- StringRequirement
    string_min_length INTEGER,
    string_max_length INTEGER,
    string_ascii_only BOOLEAN,

    -- UINT32Requirement
    uint32_min INTEGER,
    uint32_max INTEGER,

    -- INT32Requirement
    int32_min INTEGER,
    int32_max INTEGER,

    -- RGBARequirement
    rgba_uses_alpha BOOLEAN,

    -- PercentageRequirement
    percentage_min INTEGER,
    percentage_max INTEGER,
    percentage_step INTEGER,

    -- FileRequirement
    file_max_size INTEGER,
    file_type TEXT,

    -- DoubleRequirement
    double_min REAL,
    double_max REAL,

    PRIMARY KEY (
        device_id,
        capability_key,
        parameter_key
    ),

    FOREIGN KEY (device_id, capability_key)
        REFERENCES DeviceCapabilities(device_id, capability_key)
        ON DELETE CASCADE
);


-- =========================================================
-- Alarms
-- =========================================================

CREATE TABLE Alarms (
    alarm_id TEXT PRIMARY KEY NOT NULL,
    label TEXT NOT NULL,

    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    monday BOOLEAN NOT NULL DEFAULT FALSE,
    tuesday BOOLEAN NOT NULL DEFAULT FALSE,
    wednesday BOOLEAN NOT NULL DEFAULT FALSE,
    thursday BOOLEAN NOT NULL DEFAULT FALSE,
    friday BOOLEAN NOT NULL DEFAULT FALSE,
    saturday BOOLEAN NOT NULL DEFAULT FALSE,
    sunday BOOLEAN NOT NULL DEFAULT FALSE
);


-- =========================================================
-- Alarm Phases
-- =========================================================

CREATE TABLE Phases (
    phase_id TEXT PRIMARY KEY NOT NULL,
    alarm_id TEXT NOT NULL,

    label TEXT NOT NULL,

    -- Minutes since midnight: 0 - 1439
    trigger_time INTEGER NOT NULL
        CHECK (
            trigger_time >= 0
            AND trigger_time < 1440
        ),

    FOREIGN KEY (alarm_id)
        REFERENCES Alarms(alarm_id)
        ON DELETE CASCADE
);


-- =========================================================
-- Actions
--
-- device_id + device_action_key must correspond to an actual
-- capability belonging to that device.
-- =========================================================

CREATE TABLE Actions (
    action_id TEXT PRIMARY KEY NOT NULL,
    phase_id TEXT NOT NULL,

    label TEXT NOT NULL,

    device_id TEXT NOT NULL,
    device_action_key TEXT NOT NULL,

    FOREIGN KEY (phase_id)
        REFERENCES Phases(phase_id)
        ON DELETE CASCADE,

    FOREIGN KEY (device_id, device_action_key)
        REFERENCES DeviceCapabilities(device_id, capability_key)
        ON DELETE RESTRICT
);


-- =========================================================
-- Files
--
-- Stored separately rather than embedding the BLOB directly
-- inside every ActionParameter.
-- =========================================================

CREATE TABLE Files (
    file_id TEXT PRIMARY KEY NOT NULL,

    filename TEXT NOT NULL,
    file_type TEXT NOT NULL,

    size_bytes INTEGER NOT NULL
        CHECK (size_bytes >= 0),

    file_content BLOB NOT NULL
);


-- =========================================================
-- Action Parameters
--
-- Corresponds to ActionValue.oneof value.
--
-- value_type determines which value column is populated.
-- =========================================================

CREATE TABLE ActionParameters (
    parameter_id TEXT PRIMARY KEY NOT NULL,

    action_id TEXT NOT NULL,

    parameter_key TEXT NOT NULL,
    label TEXT NOT NULL,
    units TEXT,

    value_type TEXT NOT NULL
        CHECK (
            value_type IN (
                'STRING',
                'UINT32',
                'INT32',
                'BOOL',
                'RGBA',
                'PERCENTAGE',
                'FILE',
                'DOUBLE'
            )
        ),

    string_val TEXT,

    uint32_val INTEGER
        CHECK (
            uint32_val IS NULL
            OR (
                uint32_val >= 0
                AND uint32_val <= 4294967295
            )
        ),

    int32_val INTEGER
        CHECK (
            int32_val IS NULL
            OR (
                int32_val >= -2147483648
                AND int32_val <= 2147483647
            )
        ),

    bool_val BOOLEAN,

    rgba_val INTEGER
        CHECK (
            rgba_val IS NULL
            OR (
                rgba_val >= 0
                AND rgba_val <= 4294967295
            )
        ),

    percentage_val INTEGER,

    file_id TEXT,

    double_val REAL,

    FOREIGN KEY (action_id)
        REFERENCES Actions(action_id)
        ON DELETE CASCADE,

    FOREIGN KEY (file_id)
        REFERENCES Files(file_id)
        ON DELETE RESTRICT
);

-- =========================================================
-- Indexes
-- =========================================================

CREATE INDEX idx_devices_controller
    ON Devices(controller_id);

CREATE INDEX idx_phases_alarm
    ON Phases(alarm_id);

CREATE INDEX idx_actions_phase
    ON Actions(phase_id);

CREATE INDEX idx_actions_device_capability
    ON Actions(device_id, device_action_key);

CREATE INDEX idx_action_parameters_action
    ON ActionParameters(action_id);