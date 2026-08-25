# FreshCart — shared design & architecture spec

All three samples implement the same product, **FreshCart** (a quick-commerce grocery shop),
in three stacks. This file is the single source of truth: the apps must match it and each
other. Reference: the grocery-grid mock (search pill → chip row → "Fresh Items" grid).

## Design language

**Read**: consumer quick-commerce, clean airy card language, single deep-green accent,
translated natively per platform (Material 3 on Android/CMP, SwiftUI on iOS). No web-isms.

### Tokens

| Token | Value | Use |
|---|---|---|
| `ground` | `#F4F6F3` (near-white, faint green-grey tint) | screen background |
| `surface` | `#FFFFFF` | cards, search field, chips |
| `ink` | `#171A17` | primary text, prices |
| `muted` | `#8A8F8A` | secondary text, MRP strikethrough, "See All" |
| `accent` | `#1E3B2C` (deep forest green) | add button, Place Order CTA, cart badge, selected chip |
| `onAccent` | `#FFFFFF` | icons/text on accent |
| `favorite` | `#E0489B` | heart toggle only (semantic, not a second accent) |
| `badgeBg` / `badgeText` | `#E7EAFB` / `#5261C6` | weight pill ("300g") only (semantic) |
| `fresh` | `#3DA35D` | the tiny "10 MINS" dot only (semantic) |

Single accent lock: `accent` is the only interactive brand color. `favorite`/`badge`/`fresh`
are fixed semantic colors and never used on buttons or emphasis.

### Shape & type

- **Radius scale (locked)**: cards **20**, search & chips & CTAs **full pill**, small badges
  **6**. Nothing else.
- **Type**: platform system font. Product name **semibold 15**, price **bold 17**,
  MRP **12 strikethrough muted**, section header **bold 20**, "10 MINS" **10 uppercase
  muted** with `fresh` dot, weight badge **11 medium**.
- Cards: white, radius 20, very soft shadow (or 1dp tonal elevation on Android). No borders.

### Product art

Large emoji (~64pt) centered in the card's upper area on `surface` — deliberate,
dependency-free placeholder for photography; noted in the README. One emoji per product,
consistent across all three apps.

### Motion

150–200 ms, purposeful only: add-button press feedback (scale ~0.96), cart badge count
change animates, screen transitions platform-default. **No decorative or looping animation.**

## Screens & behavior

### 1. Shop (root)
- Top bar: "FreshCart" title (bold) + trailing icons: printer (opens Printer settings — the
  persistent entry point for configuring or switching printers) and cart with count badge
  (`accent` circle).
- Search pill: magnifier + "Search for groceries…". Live substring filter on product name.
- Chip row (horizontal): **Sort By** (toggles price low→high → high→low → off),
  **Category** (cycles All → Fruits → Vegetables), **Offers** (filters discounted items).
  Selected chip = `accent` fill with `onAccent` text. No dead/decorative chips.
- Section header: "Fresh Items". (No "See All" — no dead links.)
- 2-column grid of product cards: emoji art, heart toggle (top-right, outline → filled
  `favorite`), weight badge, name, `fresh`-dot + "10 MINS", price + struck MRP, and a
  circular `accent` add button (bottom-right). Tapping add increments cart (badge updates);
  if the item is already in the cart the button shows the count instead of "+" and tapping
  still increments.
- Empty search result: friendly empty state ("No groceries match…" + clear-search action).

### 2. Cart
- Line items: emoji, name + weight, unit price, qty stepper (− / count / +; − at 1 removes),
  line total. Swipe-to-delete NOT required.
- Summary: Items total, "You saved ₹N" (MRP − price, only if > 0), bold **Total**.
- Primary CTA: full-width pill "Place Order" in `accent`.
  - No printer configured → navigate to Printer settings (existing flow).
  - Printing → CTA shows progress ("Printing receipt…", disabled).
  - Success → success state: check, "Order #NNN placed", "Receipt printed", cart cleared,
    "Back to shop" action.
  - Failure → inline error card with the real message + Retry. Cart NOT cleared.
- Empty cart: composed empty state with "Browse groceries" action.

### 3. Printer settings
Reuse each app's existing scan / manual-entry flow (this is the SDK-demo core). Restyle
surfaces to the tokens above (white cards on `ground`, `accent` CTAs). No functional changes.

## Catalog (identical in all three apps)

| id | emoji | name | category | weight | price ₹ | mrp ₹ |
|---|---|---|---|---|---|---|
| oranges | 🍊 | Sweet Oranges | Fruits | 300g | 30 | 40 |
| apples | 🍎 | Fresh Apples | Fruits | 300g | 75 | 90 |
| bananas | 🍌 | Ripe Bananas | Fruits | 500g | 45 | 55 |
| grapes | 🍇 | Green Grapes | Fruits | 500g | 65 | 80 |
| tomatoes | 🍅 | Fresh Tomatoes | Vegetables | 300g | 20 | 30 |
| lettuce | 🥬 | Organic Lettuce | Vegetables | 300g | 25 | 30 |
| spinach | 🌿 | Baby Spinach | Vegetables | 250g | 35 | 45 |
| corn | 🌽 | Sweet Corn | Vegetables | 2 pc | 40 | 50 |

(Reference-mock typos deliberately corrected: Tomatoes, Organic.)

## Receipt format (ESC/POS via PrintBeam, identical bytes-intent in all apps)

```
        FRESHCART            ← center, bold
 Fresh groceries, printed fast   ← center
--------------------------------  divider("-")
2x Sweet Oranges 300g       ₹60   left qty+name, right line total (line(left, right))
1x Fresh Apples 300g        ₹75
--------------------------------
Items                       ₹135
You saved                    ₹35   ← only when > 0
TOTAL                       ₹135   ← bold
                                   feed(1)
        Order #042               ← center
        Thank you!               ← center
                                   feed(2), cut()
```

Order number: persisted incrementing counter (SharedPreferences / UserDefaults /
NSUserDefaults per platform), consumed at successful print.

## Architecture (clean & SOLID, deliberately not over-engineered)

- **Layers**: `model` (Product, CartItem, immutable) · `data` (ProductRepository interface +
  in-memory impl; CartStore = single source of truth exposing observable state;
  OrderNumberStore; existing printer-settings store) · `printing`
  (`ReceiptPrinter` interface + `PrintBeamReceiptPrinter` impl — screens/VMs never import
  PrintBeam types directly except via this seam) · `ui` (screens + components + theme).
- **Presentation**: unidirectional data flow. One state holder per screen exposing a single
  immutable UiState; UI sends explicit events/callbacks. Android/KMP: `ViewModel` +
  `StateFlow`. iOS: `@Observable` stores (iOS 17+), injected via `.environment`.
- **DI**: manual constructor injection through one app-scoped container created at the
  entry point. No DI framework (out of scope for a sample).
- **No** use-case classes for one-liners, no multi-module split, no persistence of the cart
  (in-memory; documented). SOLID where it earns its keep: DIP at the printing seam,
  ISP/SRP via small stores, LSP untouched.
- PrintBeam usage stays exactly the documented facade pattern: once-per-process
  `initialize`, `addManualPrinter` → stable id → `print(id) { … }`, `PrintResult` handling,
  streaming scan in settings.
