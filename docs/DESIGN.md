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

Repeated components to build once in Compose: top bar, five-destination bottom navigation, type chip, month selector, transaction row, tinted category icon, summary card, labeled form field, pill action, and budget progress bar. Use the observed sizes as a reference, not fixed dimensions for every device.

## Navigation

The bottom destinations are Home, Transactions, Add, Statistics, and Settings. Visible controls suggest Home → Wallets, Budgets, Categories, Transactions, or a transaction detail; Transactions → detail; detail → edit/delete; Settings → Wallets; Wallets → Transactions. Month chevrons change the selected month within Transactions, Statistics, and Budgets. The centered Add destination opens the transaction form. Secondary screens show back arrows. Figma does not show prototype outcomes; TASK-003 transaction outcomes are recorded below.

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

**TBD:** product name; final English, dark, and larger-screen designs; final non-content state designs; follow-up screens for categories, budgets, wallets, transfer, backup, export, and delete-all. Resolve each when its roadmap task is implemented. Wallets, budgets, charts, and data-management actions shown in Figma do not expand the current MVP.

## TASK-003 implementation decisions

The transaction list, add, and detail frames were consulted through Figma MCP. TASK-003 uses the existing light Material 3 theme, colors, and typography. Its transaction-only navigation provides list, add, detail, and edit routes; the Figma bottom destinations for Home, Statistics, and Settings are deferred with their features. The list omits the Figma monthly income/expense totals because calculations belong to TASK-004. The form omits account, and the detail view omits time and account, because the TASK-002 transaction model does not store them.

Edit reuses the add form with saved values. Save and confirmed delete return to the transaction list; back and cancel do not write. Delete uses a confirmation dialog. Category supports localized suggestions and free text; category management remains outside TASK-003. The unillustrated loading, empty, no-match, error, missing-record, and validation states follow the behavior table above. The form scrolls above the keyboard and uses the platform Material 3 date picker. All transaction surfaces use solid fills without gradients.
