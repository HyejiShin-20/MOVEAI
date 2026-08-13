import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { reportRepository } from '../api/reportRepository'
import { routes } from '../routes'
import { reportDraftStore } from '../state/reportDraft'

const placePhoto = 'https://www.figma.com/api/mcp/asset/5a47eab3-0c50-474e-96ae-570f4bdeb08e.png'
const evidencePhoto = 'https://www.figma.com/api/mcp/asset/d04e5861-5549-42f3-8ad6-c9860cfeb1b9.png'
const submitIcon = 'https://www.figma.com/api/mcp/asset/650b48d8-1c0f-46a4-bcab-f0e5e05d9890.svg'
const backIcon = 'https://www.figma.com/api/mcp/asset/531882f2-ac18-40f7-bfcd-1a3bb6c9cec1.svg'
const reviewIcon = 'https://www.figma.com/api/mcp/asset/986ae189-41ef-4091-ad42-efe775e6d73a.svg'
const locationIcon = 'https://www.figma.com/api/mcp/asset/03dcca41-3590-4c94-ac1e-916377e8ba48.svg'
const addressIcon = 'https://www.figma.com/api/mcp/asset/d8a7499b-5745-4e87-adf0-0e1ab4179628.svg'
const zoneIcon = 'https://www.figma.com/api/mcp/asset/2d226605-6bab-49a5-bffa-9684fccda3d1.svg'
const truckIcon = 'https://www.figma.com/api/mcp/asset/b1b0a424-b08b-4cc2-b66c-c6a07006ac93.svg'
const textIcon = 'https://www.figma.com/api/mcp/asset/da909d6e-c154-4636-828d-650a2bb3086d.svg'
const cameraIcon = 'https://www.figma.com/api/mcp/asset/4fc2ad3d-d7e8-4738-aeac-21a7065ac2be.svg'
const addIcon = 'https://www.figma.com/api/mcp/asset/b8b81c4e-f09e-4bda-8cc1-de67a19c51e7.svg'

export function ReportConfirmPage() {
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const draft = reportDraftStore.get()
  const selectedPlace = draft.selectedPlace ?? { id: 0, name: '장소 미선택', address: '장소를 선택해 주세요.' }
  const selectedZone = draft.selectedZone ?? '세부 위치 미선택'
  const transcript = draft.transcript

  const submitReport = async () => {
    if (submitting) return
    setSubmitting(true)
    setSubmitError('')
    try {
      await reportRepository.submit(reportDraftStore.get())
      reportDraftStore.clear()
      navigate(routes.myReports)
    } catch (reason) {
      setSubmitError(reason instanceof Error ? reason.message : '제보를 등록하지 못했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mobile-page confirm-page" data-figma-node="118:8176">
      <header className="transaction-header">
        <button type="button" aria-label="뒤로가기" onClick={() => navigate(routes.reportPlace)}><img src={backIcon} alt="" /></button>
        <strong>제보 확인</strong><span />
      </header>
      <main className="confirm-main">
        <section className="confirm-intro">
          <div className="confirm-eyebrow"><img src={reviewIcon} alt="" />최종 검토</div>
          <h1>등록 전 내용을<br />확인해 주세요.</h1>
          <p>잘못된 정보가 없는지 마지막으로 확인 후 제출 버튼을 눌러주세요.</p>
        </section>
        <section className="confirm-card confirm-card--place">
          <div className="confirm-card__head"><span><img src={locationIcon} alt="" />선택한 장소</span><button type="button" onClick={() => navigate(routes.reportPlace)}>수정</button></div>
          <div className="confirm-place-row">
            <img className="confirm-place-row__photo" src={placePhoto} alt={selectedPlace.name} />
            <div><strong>{selectedPlace.name}</strong><span><img src={addressIcon} alt="" />{selectedPlace.address}</span></div>
          </div>
        </section>
        <section className="confirm-card">
          <div className="confirm-card__head"><span><img src={zoneIcon} alt="" />세부 구역</span><button type="button" onClick={() => navigate(routes.reportPlace)}>수정</button></div>
          <strong className="confirm-card__value">{selectedZone}</strong>
          <span className="confirm-chip"><img src={truckIcon} alt="" />1.5t 진입가능</span>
        </section>
        <section className="confirm-card">
          <div className="confirm-card__head"><span><img src={textIcon} alt="" />수정/제보 내용</span><button type="button" onClick={() => navigate(routes.reportTranscription)}>수정</button></div>
          <blockquote>“{transcript}”</blockquote>
        </section>
        <section className="confirm-card">
          <div className="confirm-card__head"><span><img src={cameraIcon} alt="" />첨부 사진 (1건)</span></div>
          <div className="confirm-photos"><img src={evidencePhoto} alt="첨부된 현장 사진" /><button type="button" aria-label="사진 추가"><img src={addIcon} alt="" /><span>사진 추가</span></button></div>
        </section>
        <div className="confirm-spacer" />
      </main>
      <footer className="confirm-actions">
        {submitError && <p className="confirm-submit-error" role="alert">{submitError}</p>}
        <button className="confirm-submit" type="button" disabled={submitting} onClick={submitReport}><img src={submitIcon} alt="" />{submitting ? '등록 중...' : '제보 등록하기'}</button>
        <button className="confirm-cancel" type="button" onClick={() => navigate(routes.home)}>취소</button>
      </footer>
    </div>
  )
}
