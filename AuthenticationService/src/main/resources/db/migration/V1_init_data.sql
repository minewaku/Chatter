INSERT INTO public.role (id, code, name, description, is_deleted, created_date)
VALUES (
    1445089180648505345, 
    'ADMIN', 
    'ROLE_ADMIN', 
    'Administrator Role', 
    false, 
    NOW()
) ON CONFLICT (code) DO NOTHING;


INSERT INTO public."user" (
    id, 
    username, 
    email, 
    birthday, 
    is_deleted, 
    is_enabled, 
    is_locked, 
    created_date
)
VALUES (
    1445089180648505345, 
    'admin', 
    'admin@example.com', 
    '1990-01-01',
    false, 
    true, 
    false, 
    NOW()
) ON CONFLICT (username) DO NOTHING;


INSERT INTO public.user_role (user_id, role_id, is_deleted)
VALUES (
    1445089180648505344, 
    1445089180648505345, 
    false
) ON CONFLICT (user_id, role_id) DO NOTHING;


INSERT INTO public.credentials (
    user_id, 
    algorithm, 
    salt, 
    hashed_password, 
    created_date
)
VALUES (
    1445089180648505345, 
    'argon2id',
    '\xefbfbd540fefbfbdefbfbd6e784f66efbfbd79efbfbdefbfbd363fefbfbd',
    '$argon2id$v=19$m=15360,t=2,p=1$gFQPn4JueE9mnHmnmzY/sA$AmmCO6m8KWibPyCxaclcIJFH/zAecbBquPiQR2CUX9k',
    NOW()
) ON CONFLICT (user_id) DO NOTHING;