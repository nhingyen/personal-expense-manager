# TASK-015: Launcher Icon trước Release v1.0.0

Ngày xác minh: 2026-10-28.

## Phạm vi và tiêu chí chấp nhận

- Kiểm tra toàn bộ `ic_launcher`/`ic_launcher_round`, foreground/background,
  mật độ ảnh và tham chiếu trong manifest, source và APK đã build.
- Giữ resource hợp lệ; không thay UI, tính năng tài chính, Room, versionCode
  hoặc cấu hình ký; không truy cập keystore/mật khẩu.
- `:app:assembleDebug` và `:app:assembleRelease` thành công.
- Kiểm tra logo trên emulator nếu khả dụng; ghi riêng giới hạn kiểm tra và
  hướng dẫn Home Screen/App Drawer. Không uninstall hoặc clear data.
- Cập nhật README/roadmap; giữ APK và output build ngoài Git.

## File đã kiểm tra

Đã đọc `AGENTS.md`, `docs/PROJECT.md`, `docs/ARCHITECTURE.md`,
`tasks/ROADMAP.md` và tài liệu audit hiện tại. Không triển khai UI hoặc thiết kế
logo mới; giữ nguyên logo người dùng đã nhập bằng Image Asset Studio.

| Đường dẫn trong `app/src/main` | Kết quả |
| --- | --- |
| `AndroidManifest.xml` | Application dùng `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`. `MainActivity` có MAIN/LAUNCHER, không ghi đè icon. Không sửa manifest. |
| `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | Cả hai là `adaptive-icon`: background `@color/ic_launcher_background`, foreground và monochrome `@mipmap/ic_launcher_foreground`. Hai XML giống nhau là hợp lệ; launcher quyết định mask. |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp` | Đủ năm Legacy Icon vuông, dùng logo ví tím mới. |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.webp` | Đủ năm Legacy Icon tròn; là biến thể riêng, không phải file trùng cần xóa. |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.webp` | Đủ năm foreground; header lossless WebP có alpha. Đã xem ảnh xxxhdpi, logo nằm giữa vùng trong suốt. |
| `res/values/ic_launcher_background.xml` | Màu nền `#EDE5F7`, đúng resource type `color`. |
| `res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml` | Hai vector Android mẫu cũ còn hợp lệ nhưng không được launcher tham chiếu. `drawable`, `mipmap` và `color` là các loại resource khác nhau; cùng tên không gây trùng ID. Giữ nguyên cả hai. |
| `res/mipmap-anydpi/` | Hai XML cũ đã được thay bằng bộ `mipmap-anydpi-v26/` trong working tree trước lượt này. minSdk 26 nên không cần khôi phục XML cũ có thể trỏ đến logo Android mẫu. |
| `ic_launcher-playstore.png` | Logo 512 × 512, đã xem trực tiếp. Là ảnh nguồn ngoài `res`, không quyết định icon khi cài APK. |
| `app/build.gradle.kts`, `.gitignore`, `app/.gitignore` | applicationId, minSdk 26, versionName 1.0.0/code 2 và cấu hình ký giữ nguyên. `app/build/` đã được ignore; bổ sung `/app/release/` để output Android Studio không vào Git. |

| Mật độ | Legacy vuông/tròn (px) | Foreground (px) |
| --- | --- | --- |
| mdpi | 48 × 48 | 108 × 108 |
| hdpi | 72 × 72 | 162 × 162 |
| xhdpi | 96 × 96 | 216 × 216 |
| xxhdpi | 144 × 144 | 324 × 324 |
| xxxhdpi | 192 × 192 | 432 × 432 |

Không phát hiện thiếu density, resource trùng cùng type/name/config hoặc tham
chiếu launcher sai. Không thêm, sửa hay xóa resource/manifest trong lượt này.

## Build và kiểm tra APK

Đã chạy:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain --max-workers=1
```

**BUILD SUCCESSFUL trong 1m 42s**, 97 tasks: 40 executed, 57 up-to-date.
Hai task assemble hoàn tất; bước `lintVitalRelease` đi kèm cũng thành công.
Không chạy lại unit test, full lint hoặc instrumentation trong task chỉ kiểm tra
launcher này; kết quả lịch sử vẫn thuộc TASK-014.

- Debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Release chưa ký: `app/build/outputs/apk/release/app-release-unsigned.apk`.
- Đối chiếu SHA-256 của từng WebP nguồn với nội dung ZIP: **15/15 khớp ở cả
  hai APK**, gồm mười Legacy Icon và năm foreground.
- `aapt dump badging`/`xmltree`, `aapt2 dump resources` xác nhận manifest đã đóng
  gói trỏ đến `mipmap/ic_launcher` và `mipmap/ic_launcher_round`; hai adaptive
  XML dùng đúng color/foreground/monochrome. Release rút gọn tên file thành
  `res/BW.xml` và `res/0K.xml`; đã đọc cả hai, đây là tối ưu đóng gói bình thường.
- Cả hai APK giữ package `com.ngyennhi.expensemanager`, minSdk 26, targetSdk 37,
  versionName `1.0.0`, versionCode `2`.

Log kiểm tra và ảnh chụp nằm cục bộ tại `app/build/launcher-icon-audit/`, được
Git ignore. Không đưa APK hoặc ảnh/output kiểm tra từ thư mục build vào Git.

## Emulator

`emulator-5554`, AVD **Pixel_5 / API 35**, đã boot và phản hồi. Trước cài đặt,
`pm path` và `pm list packages -u` đều không có package ứng dụng. Đã chạy:

```powershell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

Kết quả **Success**. Mở launcher bằng phím Home, vuốt lên App Drawer và xem
ảnh chụp `app/build/launcher-icon-audit/app-drawer.png`: **logo ví tím trên nền
tím nhạt hiển thị đúng, dưới nhãn Personal Expense Manager bị launcher rút gọn**.
Không thấy logo Android mẫu hoặc phần chính của logo bị cắt ở mask tròn này.

Không uninstall, clear data hoặc chạy instrumentation có thể tự gỡ app. Giữ
ứng dụng Debug đã cài trên emulator. Vì trước đó không có package, lượt này
không chứng minh giữ dữ liệu qua upgrade hoặc refresh icon cache của bản cũ.

`emulator-5556` được ADB liệt kê nhưng các lệnh boot/API/package hết thời gian
chờ; không cài app hoặc tác động dữ liệu trên thiết bị này. Không coi đó là
bằng chứng runtime API 26. Legacy bitmap đã xác minh trong source/APK, chưa được
quan sát riêng trên launcher dùng Legacy Icon. Home Screen và themed icon chưa
được kiểm tra trực tiếp.

## Kiểm tra thủ công và Signed Release APK tiếp theo

1. Trên bản Debug vừa cài ở Pixel_5, mở App Drawer, tìm Personal Expense Manager,
   nhấn giữ rồi kéo shortcut ra Home Screen. Xem logo ở cả hai nơi, kiểm tra độ
   nét, nền, vị trí và phần ví/biểu đồ không bị cắt. Nhấn icon để kiểm tra mở app.
2. Nếu launcher hỗ trợ, xem thêm mask khác và bật/tắt Themed icons trên API 33+.
   Nếu icon cũ còn do cache sau cập nhật, thử về Home/mở lại drawer hoặc khởi
   động lại thiết bị. Không xóa storage của app/launcher, không uninstall để
   làm mới icon.
3. Khi tạo Signed Release APK trong Android Studio, dùng source/resources hiện
   tại và key Release đang sử dụng. Không dùng APK unsigned để cài/phân phối.
   Lượt này không tạo hoặc ký APK; artifact đã được xác minh ở TASK-014 không
   chứng nhận logo trong APK sẽ ký mới.
4. Trước khi cài cập nhật lên thiết bị có dữ liệu, xác minh public certificate
   khớp bản cũ và package giữ nguyên. Bản Debug trên emulator dùng signer khác
   Release; không thay nó bằng Release qua uninstall. Dùng thiết bị/bản cài cũ
   cùng signer cho kiểm tra signed upgrade. Việc tăng versionCode cho lần
   phát hành kế tiếp do người dùng quyết định ngoài task này; không sửa code 2.
5. Trên bản Release phù hợp, ghi nhận dữ liệu hiện có, cập nhật tại chỗ bằng
   `adb install -r <signed-apk>`, rồi kiểm tra icon ở Home/App Drawer và dữ liệu
   sau mở lại. Hoàn tất signed-upgrade/data-retention, API 26 và CI/manual gates
   còn mở trong [release audit](../docs/RELEASE_AUDIT_v1.0.0.md) trước phân phối.

Launcher resources và hai build: **PASS**. App Drawer API 35: **PASS**.
Kết quả này không thay đổi quyết định **Not Ready for distribution** của TASK-014.

Tiếp nối: TASK-014 sau đó được người dùng cho phép tăng versionCode lên 3 sau
khi giữ riêng v2 đã ký. Các kết quả code 2 ở trên là lịch sử của TASK-015; quy
trình ký và kiểm tra APK mới nằm trong [Signed Upgrade v2 → v3](../docs/SIGNED_UPGRADE_v2_v3.md).
