import SwiftUI

// Design tokens from DESIGN.md. FreshCart ships light-only: the palette is a single
// hand-tuned light scheme (near-white ground, deep-green accent) and has no dark
// counterpart in the spec, so we lock the color scheme at the app root instead of
// letting dark mode invert card surfaces underneath fixed hex values.
extension Color {
    /// Screen background — near-white with a faint green-grey tint.
    static let fcGround = Color(hex: 0xF4F6F3)
    /// Cards, search field, chips.
    static let fcSurface = Color(hex: 0xFFFFFF)
    /// Primary text and prices.
    static let fcInk = Color(hex: 0x171A17)
    /// Secondary text, MRP strikethrough.
    static let fcMuted = Color(hex: 0x8A8F8A)
    /// The one interactive brand color: add button, Place Order, cart badge, selected chip.
    static let fcAccent = Color(hex: 0x1E3B2C)
    /// Icons/text on accent.
    static let fcOnAccent = Color(hex: 0xFFFFFF)
    /// Heart toggle only.
    static let fcFavorite = Color(hex: 0xE0489B)
    /// Weight pill background/text only.
    static let fcBadgeBg = Color(hex: 0xE7EAFB)
    static let fcBadgeText = Color(hex: 0x5261C6)
    /// The tiny "10 MINS" dot only.
    static let fcFresh = Color(hex: 0x3DA35D)

    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

/// Locked radius scale: cards 20, pills full, small badges 6. Nothing else.
enum FCRadius {
    static let card: CGFloat = 20
    static let badge: CGFloat = 6
}

extension View {
    /// White card on the ground background: radius 20, very soft shadow, no border.
    func fcCard() -> some View {
        background(Color.fcSurface)
            .clipShape(RoundedRectangle(cornerRadius: FCRadius.card, style: .continuous))
            .shadow(color: Color.fcInk.opacity(0.05), radius: 10, y: 4)
    }
}

/// Press feedback for the add button and CTAs — scale to 0.96 over ~150 ms, nothing looping.
struct PressableButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// Full-width pill CTA in accent — Place Order, Find Printer, Browse groceries.
struct AccentPillButtonStyle: ButtonStyle {
    var isEnabled: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(Color.fcOnAccent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(Color.fcAccent.opacity(isEnabled ? 1 : 0.5))
            .clipShape(Capsule())
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}
