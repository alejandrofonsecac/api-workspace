-- Preferências de interface que acompanham a conta entre dispositivos.
-- O backend guarda um documento JSON sem interpretar as chaves: o frontend define
-- quais preferências existem, seus valores e padrões. Veja .docs/preferences/README.md.
CREATE TABLE tb_user_preferences (
    user_id varchar(36) PRIMARY KEY REFERENCES tb_users(id) ON DELETE CASCADE,
    data jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL,
    CONSTRAINT user_preferences_object CHECK (jsonb_typeof(data) = 'object')
);
