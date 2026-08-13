import { useNavigate } from 'react-router-dom'

const mapImage = 'https://www.figma.com/api/mcp/asset/69a55406-9aff-4824-be44-adc4bf5f0db2.png'
const photo = 'https://www.figma.com/api/mcp/asset/7e75880e-9fdf-40ff-9f18-d85beb666d2e.png'
const photo2 = 'https://www.figma.com/api/mcp/asset/17c93e2f-6dee-48b3-89b4-35ace55fe955.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/bc932a66-92bc-4092-8368-b4ee598c7ea5.svg'
const startIcon = 'https://www.figma.com/api/mcp/asset/191e1018-a425-49c0-b91d-d23a841880e8.svg'

export function GuidancePreviewPage() {
  const navigate = useNavigate()
  const cards = [{ image: photo, label: '정차 위치' }, { image: photo2, label: '후문 사진' }, { image: photo, label: '후문' }]
  return <div className="mobile-page guidance-preview-page">
    <header className="guidance-header"><button type="button" onClick={() => navigate('/home')}><img src={menuIcon} alt="" /></button></header>
    <main className="guidance-map-wrap"><img className="guidance-map" src={mapImage} alt="경로 미리보기 지도" />
      <div className="guidance-photo-strip">{cards.map((card, i) => <div className="guidance-photo-card" key={`${card.label}-${i}`}><img src={card.image} alt="" /><span>{card.label}</span></div>)}</div>
      <div className="guidance-start-wrap"><button type="button" onClick={() => navigate('/guidance/step')}><img src={startIcon} alt="" />안내 시작</button></div>
    </main>
  </div>
}
