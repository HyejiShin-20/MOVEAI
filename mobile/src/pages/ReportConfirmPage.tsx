import { useNavigate } from 'react-router-dom'

const placePhoto = 'https://www.figma.com/api/mcp/asset/09e07a89-51b0-489f-b27a-23a8b53eee5d.png'
const evidencePhoto = 'https://www.figma.com/api/mcp/asset/f73495bf-b413-4b61-9184-57532620444e.png'
const submitIcon = 'https://www.figma.com/api/mcp/asset/887bdb95-2ff0-4d93-a7c9-186d3169bebe.svg'
const backIcon = 'https://www.figma.com/api/mcp/asset/5e86f6cf-fa81-4c13-8f2b-2ec720b0adf9.svg'
const reviewIcon = 'https://www.figma.com/api/mcp/asset/17385eb6-6469-48d8-80cf-be3350ae673d.svg'
const locationIcon = 'https://www.figma.com/api/mcp/asset/ac0456e5-41e5-42de-84e3-3ed902d418ab.svg'
const addressIcon = 'https://www.figma.com/api/mcp/asset/fe049e91-8b7e-4cb1-aa94-3c0066ccfc10.svg'
const zoneIcon = 'https://www.figma.com/api/mcp/asset/473263eb-880f-45e2-acb1-b569ad4be41c.svg'
const truckIcon = 'https://www.figma.com/api/mcp/asset/d10bdfe8-4277-4b31-8ea9-fcc8e8f0d0b8.svg'
const textIcon = 'https://www.figma.com/api/mcp/asset/f0484cdd-cdcc-44fb-a34c-30b5024526ec.svg'
const cameraIcon = 'https://www.figma.com/api/mcp/asset/77fb844f-8df2-4186-ab5e-fe09ce9432c4.svg'
const addIcon = 'https://www.figma.com/api/mcp/asset/5647320e-0773-4fff-b035-9e3a2e30c270.svg'

export function ReportConfirmPage() {
  const navigate = useNavigate()
  return (
    <div className="mobile-page confirm-page">
      <header className="transaction-header">
        <button type="button" aria-label="뒤로가기" onClick={() => navigate('/reports/place')}><img src={backIcon} alt="" /></button>
        <strong>제보 확인</strong>
        <span />
      </header>
      <main className="confirm-main">
        <section className="confirm-intro">
          <div className="confirm-eyebrow"><img src={reviewIcon} alt="" />최종 검토</div>
          <h1>등록 전 내용을<br />확인해 주세요.</h1>
          <p>잘못된 정보가 없는지 마지막으로 확인 후 제출 버튼을 눌러주세요.</p>
        </section>

        <section className="confirm-card confirm-card--place">
          <div className="confirm-card__head"><span><img src={locationIcon} alt="" />선택한 장소</span><button type="button" onClick={() => navigate('/reports/place')}>수정</button></div>
          <div className="confirm-place-row">
            <img className="confirm-place-row__photo" src={placePhoto} alt="강남 물류센터 A동" />
            <div><strong>강남 물류센터 A동</strong><span><img src={addressIcon} alt="" />서울 강남구 테헤란로 123</span></div>
          </div>
        </section>

        <section className="confirm-card">
          <div className="confirm-card__head"><span><img src={zoneIcon} alt="" />세부 구역</span><button type="button">수정</button></div>
          <strong className="confirm-card__value">지하 2층 하역장 B구역</strong>
          <span className="confirm-chip"><img src={truckIcon} alt="" />1.5t 진입가능</span>
        </section>

        <section className="confirm-card">
          <div className="confirm-card__head"><span><img src={textIcon} alt="" />수정/제보 내용</span><button type="button">수정</button></div>
          <blockquote>“기존 안내된 지하 1층 하역장은 현재 공사 중으로 출입이 불가능합니다. 임시로 지하 2층 B구역을 사용해야 하며, 화물 엘리베이터 3호기를 이용하세요.”</blockquote>
        </section>

        <section className="confirm-card">
          <div className="confirm-card__head"><span><img src={cameraIcon} alt="" />첨부 사진 (1건)</span></div>
          <div className="confirm-photos">
            <img src={evidencePhoto} alt="첨부된 현장 사진" />
            <button type="button"><img src={addIcon} alt="" /><span>사진 추가</span></button>
          </div>
        </section>
        <div className="confirm-spacer" />
      </main>
      <footer className="confirm-actions">
        <button className="confirm-submit" type="button" onClick={() => navigate('/reports/mine')}><img src={submitIcon} alt="" />제보 등록하기</button>
        <button className="confirm-cancel" type="button" onClick={() => navigate('/home')}>취소</button>
      </footer>
    </div>
  )
}
