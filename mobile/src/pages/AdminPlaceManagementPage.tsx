import { useState } from 'react'
import { AdminShell } from '../components/AdminShell'

const assets = {
  map: 'https://www.figma.com/api/mcp/asset/c996da4a-bbde-4ef5-9288-0ee467926750.png',
  parking: 'https://www.figma.com/api/mcp/asset/07b7c5a9-c44f-430d-900d-e9ecf989de09.png',
  add: 'https://www.figma.com/api/mcp/asset/c617775c-5a74-4c54-9468-df2409974e11.svg',
  building: 'https://www.figma.com/api/mcp/asset/e3db239c-5702-45ea-8ebd-1d84addd1a71.svg',
  block: 'https://www.figma.com/api/mcp/asset/0e7593de-f4e9-4b19-b878-3483f4fce561.svg',
  entrance: 'https://www.figma.com/api/mcp/asset/3969ac66-f27f-4f69-9bc1-3e0e605bacb2.svg',
  parkingPin: 'https://www.figma.com/api/mcp/asset/850331bd-3a0a-4eca-b974-fa888e927a57.svg',
  elevator: 'https://www.figma.com/api/mcp/asset/2e25d8ac-eeb9-4e90-8e7e-a93e5efcba11.svg',
  marker: 'https://www.figma.com/api/mcp/asset/286e6d87-1acd-41a2-a3eb-0c7e34901a62.svg',
  upload: 'https://www.figma.com/api/mcp/asset/139991d9-c31a-4df7-b7a4-8e9786ca1165.svg',
} as const

const treeItems = [
  { name: '정문', icon: assets.entrance },
  { name: '지하 1층 정차위치 A', icon: assets.parkingPin },
  { name: '1-2호기 엘리베이터', icon: assets.elevator },
]

export function AdminPlaceManagementPage() {
  const [selected, setSelected] = useState('지하 1층 정차위치 A')
  const [indoor, setIndoor] = useState(true)
  const [saved, setSaved] = useState(false)

  return (
    <AdminShell active="places" topbarTitle="장소 및 내부 지점 관리 (A-07)">
      <div className="place-admin-page">
        <section className="place-tree-panel">
          <header><h1>장소 계층</h1><button type="button"><img src={assets.add} alt="지점 추가" /></button></header>
          <div className="place-tree">
            <div className="place-tree-root"><span>⌄</span><img src={assets.building} alt="" /><strong>래미안 퍼스티지</strong></div>
            <div className="place-tree-branch">
              <div className="place-tree-block"><span>⌄</span><img src={assets.block} alt="" /><strong>101동</strong></div>
              <div className="place-tree-leaves">
                {treeItems.map((item) => <button key={item.name} type="button" className={selected === item.name ? 'is-selected' : ''} onClick={() => setSelected(item.name)}><span>—</span><img src={item.icon} alt="" />{item.name}</button>)}
              </div>
              <div className="place-tree-block is-muted"><span>›</span><img src={assets.block} alt="" /><strong>102동</strong></div>
            </div>
          </div>
        </section>

        <section className="place-map-detail">
          <div className="place-map-card">
            <img className="place-map-card__image" src={assets.map} alt="서울 지도" />
            <div className="place-map-marker"><img src={assets.marker} alt="" /><span>{selected}</span></div>
            <div className="place-map-zoom"><button type="button">＋</button><button type="button">−</button></div>
          </div>

          <form className="place-detail-card" onSubmit={(e) => { e.preventDefault(); setSaved(true) }}>
            <div className="place-detail-head"><div><h2>지점 상세 정보</h2><span>AI 분석 신뢰도: 94%</span></div><div><button type="button" className="outline">새 지점 등록</button><button className="primary" type="submit">저장</button></div></div>
            <div className="place-detail-grid">
              <label><span>지점명</span><input value={selected} onChange={(e) => setSelected(e.target.value)} /></label>
              <label><span>지점 유형</span><select defaultValue="parking"><option value="parking">정차 위치 (주차장)</option><option>출입구</option><option>엘리베이터</option></select></label>
              <fieldset><legend>실내외 구분</legend><label><input type="radio" checked={!indoor} onChange={() => setIndoor(false)} /> 실외</label><label><input type="radio" checked={indoor} onChange={() => setIndoor(true)} /> 실내 (지하 포함)</label></fieldset>
              <label><span>접근 제한 사항</span><input defaultValue="탑차 진입 가능 (높이 2.3m)" /></label>
              <label className="place-photo-field"><span>현장 사진</span><div><img src={assets.parking} alt="정차 위치 현장 사진" /><button type="button"><img src={assets.upload} alt="" />사진 변경</button></div></label>
            </div>
            {saved && <p className="place-save-note">저장되었습니다.</p>}
          </form>
        </section>
      </div>
    </AdminShell>
  )
}
