import { useNavigate } from 'react-router-dom'

const mapImage = 'https://www.figma.com/api/mcp/asset/3b0a65b7-25b6-4ffc-a1eb-6a31f00ca4c5.png'
const stopPhoto = 'https://www.figma.com/api/mcp/asset/6a3cb7d3-559a-4699-a6c5-08baa2d1ddc5.png'
const backGatePhoto = 'https://www.figma.com/api/mcp/asset/ac1cc887-4fc3-4dac-9f06-a428907c62d4.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/482f2379-ef48-42a9-95b6-93591c4fc8d4.svg'
const startIcon = 'https://www.figma.com/api/mcp/asset/0fa96c88-41c3-44ff-8aa6-2ccb294c83a8.svg'

const photoCards = [
  { image: stopPhoto, label: '정차 위치' },
  { image: backGatePhoto, label: '후문 사진' },
  { image: backGatePhoto, label: '후문 사진' },
]

export function GuidancePreviewPage() {
  const navigate = useNavigate()

  return (
    <div className="mobile-page guidance-preview-page" data-figma-node="118:9145">
      <header className="guidance-header guidance-header--plain">
        <button type="button" aria-label="이전 화면" onClick={() => navigate('/home')}>
          <img src={menuIcon} alt="" />
        </button>
      </header>

      <main className="guidance-map-wrap">
        <img className="guidance-map" src={mapImage} alt="Last 100m 경로 미리보기 지도" />

        <div className="guidance-photo-strip" aria-label="경로 참고 사진">
          {photoCards.map((card, index) => (
            <article className="guidance-photo-card" key={`${card.label}-${index}`}>
              <img src={card.image} alt="" />
              <span>{card.label}</span>
            </article>
          ))}
        </div>

        <div className="guidance-start-wrap">
          <button type="button" onClick={() => navigate('/guidance/step')}>
            <img src={startIcon} alt="" />
            <span>안내 시작</span>
          </button>
        </div>
      </main>
    </div>
  )
}
