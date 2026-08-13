import { useNavigate } from 'react-router-dom'
import { routes } from '../routes'
import { reportDraftStore } from '../state/reportDraft'

const closeIcon = 'https://www.figma.com/api/mcp/asset/bf2f9e73-8689-4d59-9fd4-ea11aa50de12.svg'
const pauseIcon = 'https://www.figma.com/api/mcp/asset/ad55ad36-4667-4960-aacf-333bb8b0a163.svg'
const finishIcon = 'https://www.figma.com/api/mcp/asset/fc66d954-51ed-46e6-82fe-8820a45d2015.svg'

const waveform = [
  ['100%', 0.4], ['74%', 0.6], ['49%', 0.5], ['100%', 0.8],
  ['82%', 1], ['100%', 1], ['66%', 1], ['33%', 1], ['79%', 1], ['100%', 1],
  ['49%', 0.4], ['25%', 0.3], ['74%', 0.6], ['100%', 0.8], ['66%', 0.5], ['33%', 0.3],
] as const

export function VoiceRecordPage() {
  const navigate = useNavigate()

  return (
    <div className="mobile-page voice-record-page">
      <header className="record-header">
        <button className="record-header__button" type="button" aria-label="닫기" onClick={() => navigate(routes.home)}>
          <img src={closeIcon} alt="" />
        </button>
        <h1>현장 팁 녹음</h1>
        <span className="record-header__spacer" aria-hidden="true" />
      </header>

      <main className="voice-record-main">
        <section className="record-guide">
          <p>방금 배송한 장소의 현장 정보를 말씀해 주세요.</p>
          <strong>{'예: "1.5톤 탑차는 정문 진입이 어렵고..."'}</strong>
        </section>

        <section className="record-wave" aria-label="녹음 중 15초">
          <strong>00:15</strong>
          <div className="waveform" aria-hidden="true">
            {waveform.map(([height, opacity], index) => (
              <span
                key={`${height}-${index}`}
                className={index >= 4 && index <= 9 ? 'waveform__bar waveform__bar--active' : 'waveform__bar'}
                style={{ height, opacity }}
              />
            ))}
          </div>
        </section>

        <section className="record-actions">
          <button className="record-action record-action--pause" type="button" disabled>
            <img src={pauseIcon} alt="" />
            <span>일시정지</span>
          </button>
          <button className="record-action record-action--finish" type="button" onClick={() => { reportDraftStore.patch({ stage: 'uploading' }); navigate(routes.reportUploading) }}>
            <img src={finishIcon} alt="" />
            <span>기록 완료</span>
          </button>
        </section>
      </main>
    </div>
  )
}
