const items = [
  { label: '배송', icon: '/assets/nav-delivery.svg' },
  { label: '현장 팁', icon: '/assets/nav-tip.svg' },
  { label: '경로안내', icon: '/assets/nav-route.svg' },
  { label: 'MY', icon: '/assets/nav-my.svg', compact: true },
]

export function BottomNav() {
  return (
    <nav className="bottom-nav" aria-label="주요 메뉴">
      {items.map((item) => (
        <button className="bottom-nav__item" type="button" key={item.label}>
          <span className={`bottom-nav__icon${item.compact ? ' bottom-nav__icon--compact' : ''}`}>
            <img src={item.icon} alt="" />
          </span>
          <span>{item.label}</span>
        </button>
      ))}
    </nav>
  )
}
