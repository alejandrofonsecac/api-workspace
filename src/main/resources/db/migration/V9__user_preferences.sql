-- Preferências de interface que acompanham a conta entre dispositivos.
-- Sem linha para o usuário, a API devolve os valores padrão.
CREATE TABLE tb_user_preferences (
    user_id varchar(36) PRIMARY KEY REFERENCES tb_users(id) ON DELETE CASCADE,
    theme varchar(20) NOT NULL,
    accent varchar(20) NOT NULL,
    planner_display varchar(20) NOT NULL,
    sleep_enabled boolean NOT NULL,
    sleep_start smallint NOT NULL,
    sleep_end smallint NOT NULL,
    sidebar_width smallint NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT user_preferences_theme CHECK (theme IN ('light', 'dark', 'high-contrast', 'system')),
    CONSTRAINT user_preferences_accent CHECK (accent IN ('indigo', 'teal', 'orange', 'pink', 'blue')),
    CONSTRAINT user_preferences_planner_display CHECK (planner_display IN ('compact', 'periods', 'radial')),
    CONSTRAINT user_preferences_sleep_hours CHECK (sleep_start BETWEEN 0 AND 23 AND sleep_end BETWEEN 0 AND 23),
    CONSTRAINT user_preferences_sidebar_width CHECK (sidebar_width BETWEEN 240 AND 340)
);
