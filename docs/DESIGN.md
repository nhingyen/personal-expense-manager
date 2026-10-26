# Design reference

The [Figma file](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=0-1) contains nine Vietnamese Android screens, each 393 × 852 px. Measurements below are observed layer values, not named Figma variables or invented design tokens. The file shows populated, light appearance screens only. **TBD: final product name**; the mockups say PocketBalance, while the repository says Personal Expense Manager.

## Screens

English names below are proposed translations; Figma has no English frames.

| Vietnamese / English | Figma frame | Main content |
| --- | --- | --- |
| Trang chủ / Home | [3:9610](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-9610) | Balance, monthly income and expense, budget progress, recent transactions. |
| Giao dịch / Transactions | [3:9699](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-9699) | Search, type filters, month selector, date-grouped list. |
| Thêm giao dịch / Add transaction | [3:9803](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-9803) | Type, amount, category, date, note, account, save. |
| Chi tiết giao dịch / Transaction details | [3:9871](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-9871) | Record fields, edit, delete. |
| Thống kê / Statistics | [3:9943](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-9943) | Monthly totals, category donut, weekly bars. |
| Danh mục / Categories | [3:10049](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-10049) | Expense and income category grids. |
| Ngân sách / Budgets | [3:10148](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-10148) | Monthly and category budget progress. |
| Cài đặt / Settings | [3:10256](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-10256) | Language, theme, wallet, currency, backup, export, deletion. |
| Ví & Tài khoản / Wallets & accounts | [14:189](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=14-189) | Balance, cash and bank wallets, transfer, recent transactions. |

## Observed visual values

| Visible use | Exact Figma color |
| --- | --- |
| Background; white cards | #FAF9FC; #FFFFFF |
| Primary; secondary text | #292635; #777280 |
| Purple action/text/surface | #77609C; #624A88; #EDE5F7 |
| Income text/surface | #3F7A63; #E0F1E9 |
| Expense text/surface | #A65C56; #F9E4E1 |
| Yellow accent/surface | #8B7538; #F8EFCD |
| Search or progress track; border | #F0EDF4; #EAE6EF |
| Chart accents | #B9A4D7; #EAB4AC; #A8D3C0; #E8D58D |
| Bank wallet icon surface | #E5EDF7 |

**Project rule:** use muted pastel colors and solid fills only; no gradients. Dark text and action colors above provide contrast. The Figma colors are observed values, not a named token system.

All sampled text uses Roboto Regular, Medium, or SemiBold at width axis 100; sampled line height is 1.35 × font size.

| Use | Measured size and weight |
| --- | --- |
| Screen title | 24 px SemiBold |
| Balance / entry amount / budget total | 36 px SemiBold / 38 px Medium / 26 px SemiBold |
| Greeting / section heading / chart heading / chart center | 20 px Medium / 18 px SemiBold / 17 px SemiBold / 16 px SemiBold |
| Row and field text | 14 px, weight varies by emphasis |
| Chip and field label / supporting text / navigation label | 13 px / 12 px / 11 px; Regular, Medium, or SemiBold by state |

| Element | Measured size or spacing |
| --- | --- |
| Frame sections | Status 28 px; title 64 px; content 656 px; bottom navigation 80 px; gesture area 24 px |
| Margins and section gaps | Content sides 20 px, status sides 24 px; content top/bottom 8/16 px; Home and Add sections 16 px, Transactions sections 10 px |
| Common local gaps | 4, 8, 12 px; navigation icon to label 4 px |
| Cards | 20 px radius; 16 or 20 px padding in the shown cards |
| Search / filter chip / form field | 48 / 36 / 56 px high; form field radius 14 px and horizontal padding 16 px |
| Transaction row / category icon | 64 px high / 44 × 44 px with 14 px radius |
| Category icon in Categories / form / Budgets | 38 / 36 / 34 px square; 14 px radius |
| Save button / month selector / budget progress | 50 / 40 / 7 px high |
| Bottom selected indicator / center Add indicator | 58 × 32 / 48 × 32 px; 20 px radius |

Repeated components to build once in Compose: top bar, type chip, month selector, transaction row, tinted category icon, summary card, labeled form field, pill action, and budget progress bar. Use the observed sizes as a reference, not fixed dimensions for every device.

## Navigation

The Figma bottom destinations are Home, Transactions, Add, Statistics, and Settings. TASK-005 implements these entries: Home and Transactions reuse the existing screens, Add opens the existing form, and Statistics/Settings are localized placeholders. Home also opens Transactions through View all. Android Back from another primary tab returns to Home; the bar is hidden on add/detail/edit to keep the secondary flow focused. Visible controls suggest Home → Wallets, Budgets, Categories, Transactions, or a transaction detail; Transactions → detail; detail → edit/delete; Settings → Wallets; Wallets → Transactions. Month chevrons change the selected month within Transactions, Statistics, and Budgets. The centered Add destination opens the transaction form. Secondary screens show back arrows. Figma does not show prototype outcomes; TASK-003 transaction outcomes are recorded below.

## Localization and Compose

Vietnamese is the default resource locale; English resources and Android 13+ system language support are already described in [ARCHITECTURE.md](ARCHITECTURE.md). The Figma file contains Vietnamese copy only. **TBD: final English copy and layouts.** Add matching string/plural keys in both locales for each implemented screen. Format VND, dates, months, and percentages in presentation for the active locale; keep amounts as whole VND. The Settings mockup shows an in-app language picker, which the [roadmap](../tasks/ROADMAP.md) defers.

Use Material 3 Scaffold, navigation, cards, chips, fields, and buttons where they fit; compose the distinctive Add indicator and category icons. The shared theme uses the observed light Figma colors and Roboto styles with dynamic color disabled; a dark design remains TBD. Respect system insets, scrolling, font scaling, touch targets, and accessibility labels. Follow [ARCHITECTURE.md](ARCHITECTURE.md) for ViewModel, domain, persistence, and dependency rules.

## Loading, empty, and error states

**TBD: final visual designs and copy.** Figma shows populated screens only. The following behavior supports the [roadmap](../tasks/ROADMAP.md):

| Surface | Loading | Empty | Error |
| --- | --- | --- | --- |
| Transactions | Keep navigation visible; show progress until the first observation. | Show an Add action; distinguish no records from no search/filter matches. | Explain failure, offer Retry, retain month and filters. |
| Add/edit | Retain fields and block duplicate saves. | Start without an amount; show required-field errors. | Keep input, show the failed field or write, offer retry. |
| Detail | Show progress for the selected record. | Explain that a missing/deleted record is unavailable; offer return to Transactions. | Explain failed load or action; do not claim success. |
| Monthly summary | Show progress until values arrive. | Show zero income, expense, and balance for an empty month. | Offer Retry; identify any stale totals. |

Treat loading and an observed empty result as separate UI states. Announce errors accessibly and do not rely on color alone.

## TBD and scope

**TBD:** product name; final English, dark, and larger-screen designs; final non-content state designs; full management screens for categories/budgets, wallets, transfer, backup, export, and delete-all. Resolve each when its roadmap task is implemented. The latest TASK-004 request explicitly includes Home budget configuration/progress, category selection, and in-app budget notices; the remaining Figma features do not expand the current MVP.

## HomeScreen UI slice (2026-10-23)

Home `3:9610` was consulted again through Figma MCP design context, metadata, and screenshot. HomeScreen renders the existing HomeUiState and sends month, Add, View all, record ID, and Retry callbacks. It reuses the theme, AppTopBar, transaction row/category icons, error banner, and locale formatting. MonthSelector is shared with Transactions. The monthly card labels the primary value as net income; income/expense use Figma's two columns when their measured localized text fits, and stack only when needed for long values or larger fonts. Loading, observed-empty, error, stale data, retry, and overflow have distinct rendering; no initial financial values are fabricated.

TASK-004 integration connects the screen through `ExpenseManagerApp` with lifecycle-aware StateFlow collection and Home as root. Home and Transactions have no bottom navigation. They use the existing floating Add action and empty-state Add button; View all opens Transactions and Android Back returns to Home. Secondary form/detail/edit screens keep back controls. Home has a localized title. Row amount width is bounded so very long values wrap while leaving transaction text readable. The later Home completion below adds greeting/avatar, bell, shortcuts, and a real monthly budget. Wallets, statistics, settings, full budget/category management, and OS notifications remain deferred. VI/EN resource keys match; device verification is recorded in TASK-004.

## TASK-004 visual fidelity correction (2026-10-23)

Home `3:9610`, Transactions `3:9699`, and Add `3:9803` were re-read through Figma MCP design context and screenshots after review identified avoidable differences from the reference. This correction changes presentation only:

- Shared top bars use a 64 dp content area and 20 dp primary-title margin, with system status-bar insets handled separately. Secondary titles and back controls follow the Add reference. Shared typography explicitly retains the full 1.35× line-height frame (`LineHeightStyle.Trim.None`); Compose's default trimming had made single-line text frames and the summary card shorter than Figma. The existing solid pastel palette remains shared.
- The monthly card uses 20 dp padding/radius, 12 dp section spacing, a 36 sp net value, two equal income/expense columns separated by 12 dp, 16 dp directional icons, 12 sp month labels, and 14 sp semibold totals. Actual text measurement selects a full-width fallback; values are never rounded, shortened, or clipped to force a layout.
- The calendar and selected month form one centered group. Chevron controls retain 48 dp touch bounds. The Home completion moves this selector into a dialog opened by the greeting's date/month line; Home content starts 8 dp below the top bar with 16 dp between the greeting and card.
- Thirteen original Figma SVGs were converted to local Android vector drawables for Home, Transactions, Add, income/expense directions, month calendar/chevrons, and five category icons. They preserve the exported paths and 1.8-unit strokes, without runtime asset URLs. The replaced low-resolution category/chevron PNGs were removed; existing category drawable names remain reusable across screens.
- Recent transactions use 6 dp row gaps, 64 dp minimum rows, 44 dp category icons, and 4 dp between row text lines. A saved note is the main row title, as in Figma's lunch/coffee examples; category and stored date are supporting text, and the localized transaction type remains available to accessibility services. Records without notes retain category titles. No time/account values are invented. View all uses 12 sp medium text and a 48 dp touch height. Transactions content uses the reference 8 dp top inset and 10 dp section gaps.

The selected-month control, net-income label, localized Home title, floating Add action, and up-to-five real recent rows remain intentional scope adaptations. The earlier bottom-bar implementation and its dedicated icons were removed on 2026-10-24 at the user's request; earlier visual records do not require a bottom bar. A full nine-screen replica requires the future features listed above.

Verification on Pixel_5/API 35 uses the device's actual status/cutout inset (136 px at 440 dpi, approximately 49.5 dp), rather than Figma's 28 px status placeholder. This shifts the content vertically on that emulator while preserving the measured component geometry. Screenshot fixtures use the same edge-to-edge setup as MainActivity; device insets must remain respected.

## TASK-004 Home completion (2026-10-23)

The user explicitly requested the Home blocks that the earlier implementation omitted. Home `3:9610` was consulted again through Figma MCP design context and screenshot. The order is now top bar with bell, date/greeting/avatar, summary, Budget/Categories shortcuts, monthly budget, and recent transactions.

- The bell uses the exported 22 dp outline in a 48 dp control. The greeting uses localized device date/time, 13 sp supporting text and 20 sp medium greeting, beside a neutral 42 dp circular avatar. No name/initials are invented. The date/month line opens the shared month selector in a dialog, retaining selected-month functionality without another permanent header row.
- Shortcuts have equal widths, a 12 dp gap, 48 dp minimum height, 14 dp radius, 20 dp original Figma icons, and the existing green/yellow surfaces. Budget opens a working limit editor; Categories opens localized form suggestions and saved categories, and selection prefills Add with the category/type.
- The budget card uses a white surface, 20 dp radius, 16 dp padding, 14 sp title, 12 sp supporting text, 9 dp local gaps, and a 7 dp purple progress bar. The populated reference fixture measures approximately 92 dp high. It grows when text needs space. Percentage and remaining/exceeded amounts derive from the selected month's real expenses and saved limit. Unset budgets show a setup state; loading/errors do not show invented amounts or progress. Exceeded values remain exact even when the bar visually reaches 100%.
- The bell opens actual current-month budget notices or an explicit empty state. Editor, category chooser, and notices use small working Material 3 sheets; full management screens and OS notification delivery remain deferred. All added labels/errors use VI/EN resources. Three exported Figma shortcut/bell vectors and a neutral local avatar vector reuse the shared palette and require no runtime network access.

Reference fixture values, including 12,000,000 VND and 68%, exist only in tests. Runtime Home starts with no configured limit and uses persisted user data. Verification, local screenshots, and remaining platform/accessibility limitations are recorded in TASK-004.

## TASK-003 implementation decisions

The transaction list, add, and detail frames were consulted through Figma MCP. TASK-003 uses the existing light Material 3 theme, colors, and typography. Its transaction-only navigation provides list, add, detail, and edit routes; the Figma bottom destinations for Home, Statistics, and Settings are deferred with their features. The list omits the Figma monthly income/expense totals because calculations belong to TASK-004. The form omits account, and the detail view omits time and account, because the TASK-002 transaction model does not store them.

Edit reuses the add form with saved values. Save and confirmed delete return to the transaction list; back and cancel do not write. Delete uses a confirmation dialog. Category supports localized suggestions and free text; category management remains outside TASK-003. The unillustrated loading, empty, no-match, error, missing-record, and validation states follow the behavior table above. The form scrolls above the keyboard and uses the platform Material 3 date picker. All transaction surfaces use solid fills without gradients.

## TASK-005 bottom navigation (2026-10-24)

This task supersedes the historical bottom-navigation deferral above. Figma MCP design context and screenshots were consulted for Home bar `3:9675`, Transactions bar `3:9779`, Statistics bar `3:10025` and Settings bar `3:10322`; Add frame `3:9803` was inspected for the secondary-screen adaptation.

The shared Material 3 surface uses the existing palette/typography, a white background, top border, 8 dp outer padding, an 80 dp minimum content row, 22 dp original outline icons, 4 dp icon/label gaps and 11 sp labels. Selected tabs use a 58 x 32 dp pastel-purple indicator and semibold purple label. The centered Add action uses a 48 x 32 dp solid-purple indicator and white icon; its label remains unselected. Five exported SVG assets were downloaded and converted with Android's Svg2Vector to local vector drawables; no runtime Figma URLs or icon dependency is used.

Indicators keep their normal reference dimensions. Slots remain equal at the reference text size; measured long VI/EN labels can use spare width at larger text sizes. If all labels cannot fit, equal slots and a taller wrapped row preserve touch bounds and readable text. The complete tab/action area has at least 48 dp touch bounds. Selection uses Tab semantics; Add uses Button semantics without a selected state. System navigation and horizontal safe-area insets are handled independently of the reference content height. The host disables the duplicate floating Add actions on Home/Transactions and reduces their extra list-bottom spacing; empty-state actions remain.

Statistics/Settings contain localized titles and explicit future-feature messages only in TASK-005. No chart, settings control, fabricated data or business feature was introduced by that task. Secondary forms/detail/edit hide the bar, an intentional TASK-005 adaptation of the always-visible mockup bar. Navigation behavior and verification are recorded in [TASK-005](../tasks/TASK-005.md).

## TASK-007 Statistics & Charts (2026-10-26)

Statistics frame `3:9943` was read through Figma MCP design context and its screenshot. TASK-007 supersedes the Statistics placeholder described above. It reuses the shared top bar/month selector/bottom navigation, Figma direction icons and solid pastel palette. Content has 20 dp side margins, 16 dp section gaps, two 14 dp-radius summary cards with 12 dp padding, and white 20 dp-radius chart cards with 16 dp padding and 17 sp headings. The 140 dp donut uses a 16 dp ring and an 18 dp gap to its legend; compact center amounts are approximate while totals and category amounts remain exact. Narrow layouts and larger fonts stack the donut/legend and, when measured amounts need room, stack the summary cards. Saved category names wrap and remain unchanged when the language changes.

The requested six-month income/expense comparison replaces the reference weekly expense chart. Paired green/coral bars share a scale, include zero months, identify their date range and have exact localized text equivalents below them. Six groups fit at normal text size; the bar strip scrolls horizontally on narrow screens or with larger fonts. The whole screen scrolls vertically. Category percentages use up to one localized decimal place. Independently rounded percentages may not sum to exactly 100%. Dynamic charts use actual domain values rather than static Figma SVG slices. The compact donut-center amount is omitted when measured text cannot fit; exact amounts remain in the summary and legend. Existing local navigation/calendar/chevron/direction vectors match the reference slots; no new static assets are needed.

The reference download/export action is omitted because exporting is outside this task. Empty/income-only/comparison-empty/loading/read-error/overflow states use matching VI/EN resources; errors have Retry and hide outdated totals. No Budget, Wallet, Settings or export behavior is added. Verification and limitations are recorded in [TASK-007](../tasks/TASK-007.md).
