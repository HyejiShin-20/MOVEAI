import { useNavigate } from 'react-router-dom'
const mapImage = 'https://www.figma.com/api/mcp/asset/fca8f681-69f2-4103-bda1-42b701a14bef.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/44596786-762f-4a87-9dd6-82209e2c80c3.svg'
const locationIcon = 'https://www.figma.com/api/mcp/asset/bdec66f8-48e3-4add-a519-21535b7e2706.svg'
const plusIcon = 'https://www.figma.com/api/mcp/asset/ba0c48b1-983f-4a9a-8e54-cd0eaba37f9a.svg'
const minusIcon = 'https://www.figma.com/api/mcp/asset/ee7bcac3-a46b-4a6b-84f1-5e5baab4b656.svg'
const turnIcon = 'https://www.figma.com/api/mcp/asset/67131e08-fb5b-4d31-94b7-78241ca327d2.svg'
const endIcon = 'https://www.figma.com/api/mcp/asset/bc4eee7b-9b70-47c6-8cb4-4f03f90ef6a9.svg'
export function GuidanceStepPage() {
 const navigate=useNavigate()
 return <div className="mobile-page guidance-step-page">
  <header className="guidance-header"><button type="button" onClick={()=>navigate('/home')}><img src={menuIcon} alt="" /></button></header>
  <div className="guidance-step-map"><img src={mapImage} alt="길안내 지도" /></div>
  <div className="guidance-map-controls"><button><img src={locationIcon} alt="" /></button><div><button><img src={plusIcon} alt="" /></button><button><img src={minusIcon} alt="" /></button></div></div>
  <section className="guidance-step-card"><div className="guidance-step-card__head"><img src={turnIcon} alt="" /><strong>우회전</strong></div><div className="guidance-step-card__body"><div><strong>120m 앞</strong><span>약 2분 후</span></div><p>후문으로 차량 진입.</p></div></section>
  <button className="guidance-end" type="button" onClick={()=>navigate('/guidance/completed')}><img src={endIcon} alt="" />안내 종료</button>
 </div>
}
