interface TopAppBarProps {
  onMenu?: () => void
  onProfile?: () => void
}

export function TopAppBar({ onMenu, onProfile }: TopAppBarProps) {
  return (
    <header className="top-app-bar">
      <button className="icon-button top-app-bar__icon" type="button" aria-label="메뉴" onClick={onMenu}>
        <img src="/assets/menu.svg" alt="" />
      </button>
      <span className="top-app-bar__brand" aria-hidden="true">Logistics Pro</span>
      <button className="icon-button top-app-bar__icon" type="button" aria-label="내 정보" onClick={onProfile}>
        <img src="/assets/profile.svg" alt="" />
      </button>
    </header>
  )
}
