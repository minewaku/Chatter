auth-service/spring/
auth-service/value/
auth-service/value/jwt/rs256/public-key
auth-service/value/jwt/rs256/private-key

🧑 User-facing

IdentityService (User)
- Đăng ký tài khoản (sign up)
- Đăng nhập / đăng xuất (login/logout)
- Quản lý mật khẩu (reset/change)
- Phát hành & refresh access token
- (Optional) Multi-factor authentication

AccessControlService (role/permission trong guild/channel)
- Quản lý role trong guild (tạo/sửa/xoá role)
- Gán role cho user trong guild
- Thiết lập permission cho channel theo role/user
- Kiểm tra quyền truy cập (authorization check API)

ProfileService (User profile)
- Quản lý avatar, display name, bio
- Trạng thái online/offline/away
- User settings (theme, notification preferences)

GuildService (server/guild management)
- Tạo/xoá guild (server)
- Chỉnh sửa thông tin guild (tên, icon, region)
- Quản lý member: invite, kick, ban, assign owner
- Quản lý guild-level settings

ChannelService (channel management)
- Tạo/sửa/xoá channel trong guild
- Quản lý loại channel (text, voice, category)
- Đổi tên, topic, order của channel
- Quản lý membership/visibility của channel

MessagingService (chat)
- Gửi/nhận tin nhắn text
- Đính kèm file, ảnh, emoji
- Edit/delete tin nhắn
- Lưu lịch sử chat (message history)
- Quản lý reaction (👍, ❤️, …)


🛠️ Admin-facing

AdminIdentityService (Admin)
- Đăng nhập admin
- Phát hành token cho admin dashboard
- Quản lý session admin

AdminAccessService (permission admin)
- Quản lý role/permission cho admin
- Kiểm soát hành động quản trị (suspend user, xoá guild, v.v.)
- Audit log cho thao tác admin

| Cấp độ                        | Ý nghĩa                             | Mục đích                                                                |
| ----------------------------- | ----------------------------------- | ----------------------------------------------------------------------- |
| **TRACE**                     | Mức thấp nhất, cực kỳ chi tiết      | Dùng để theo dõi từng bước xử lý nội bộ — thường chỉ bật khi debug sâu  |
| **DEBUG**                     | Dành cho log phục vụ lập trình viên | Hiển thị luồng logic, giá trị biến, trạng thái hệ thống trong lúc dev   |
| **INFO**                      | Thông tin bình thường               | Ghi nhận sự kiện đáng chú ý: service start, user login, job completed   |
| **WARN**                      | Cảnh báo có thể gây vấn đề          | Không lỗi nhưng có dấu hiệu bất thường: retry, fallback, deprecated API |
| **ERROR**                     | Lỗi thật sự trong hệ thống          | Có exception, logic hỏng, truy vấn DB fail... nhưng app vẫn chạy được   |
| **FATAL** (hoặc **CRITICAL**) | Mức nghiêm trọng nhất               | Lỗi làm hệ thống ngừng hoạt động — thường log trước khi shutdown        |
