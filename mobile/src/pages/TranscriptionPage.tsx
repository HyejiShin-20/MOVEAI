import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const menuIcon = 'https://www.figma.com/api/mcp/asset/6cddfd86-59cb-41d0-a920-2cfd16bc5330.svg'
const profileIcon = 'https://www.figma.com/api/mcp/asset/2f5748b1-d3bf-471b-82e2-8b413c3f135f.svg'
const playIcon = 'https://www.figma.com/api/mcp/asset/6a274eff-296d-44d1-9bea-f539d0013ec2.svg'
const transcriptionIcon = 'https://www.figma.com/api/mcp/asset/6a274eff-296d-44d1-9bea-f539d0013ec2.svg'
const retryIcon = 'https://www.figma.com/api/mcp/asset/39f2d6a1-ac7a-427d-885f-51f56888d7de.svg'
const nextIcon = 'https://www.figma.com/api/mcp/asset/15a1613d-14c7-4ff0-808b-707ca22dace2.svg'
const navDelivery = 'https://www.figma.com/api/mcp/asset/c01e67b8-147f-40ae-958e-2768c3d84f1e.svg'
const navVoice = 'https://www.figma.com/api/mcp/asset/12d5d019-0a0c-4594-95e4-afac99de3314.svg'
const navNavigation = 'https://www.figma.com/api/mcp/asset/f406938c-d4d5-409c-a40d-987082ab5865.svg'
const navProfile = 'https://www.figma.com/api/mcp/asset/5366b926-2803-478d-a467-5afaf00a84b3.svg'

const waveHeights = [56, 42, 28, 56, 47, 56, 37, 19, 45, 56, 28, 14, 42, 56, 37, 19]
const initialText = '고객님, 배송 물품 문 앞에 두고 갑니다.\n공동현관 비밀번호가 틀려서 경비실 호출했는데 부재중이시라 어쩔 수 없이 1층 우편함 옆에 두었습니다. 확인 부탁드립니다.'

export function TranscriptionPage() {
  const navigate = useNavigate()
  const [text, setText] = useState(initialText)

  return (
    <div className="mobile-page transcription-page">
      <header className="transcription-header">
        <button type="button" aria-label="메뉴"><img src={menuIcon} alt="" /></button>
        <strong>Logistics Pro</strong>
        <button type="button" aria-label="프로필"><img src={profileIcon} alt="" /></button>
      </header>

      <main className="transcription-main">
        <section className="transcription-intro">
          <h1>변환 결과 확인</h1>
          <p>음성을 다음과 같이 변환했습니다. 잘못 인식된 부분이 있다면 수정해 주세요.</p>
        </section>

        <section className="audio-card">
          <div className="audio-card__head">
            <strong>ORIGINAL AUDIO</strong>
            <span>00:14</span>
          </div>
          <div className="audio-card__body">
            <button type="button" className="audio-play" aria-label="원본 음성 재생"><img src={playIcon} alt="" /></button>
            <div className="audio-waveform" aria-hidden="true">
              {waveHeights.map((height, index) => (
                <span key={`${height}-${index}`} className={index >= 4 && index <= 9 ? 'active' : ''} style={{ height }} />
              ))}
            </div>
          </div>
        </section>

        <section className="transcription-edit">
          <div className="transcription-edit__label"><img src={transcriptionIcon} alt="" /><span>AI TRANSCRIPTION</span></div>
          <textarea aria-label="음성 변환 결과" value={text} onChange={(event) => setText(event.target.value)} />
        </section>

        <section className="transcription-actions">
          <button type="button" className="transcription-action transcription-action--retry" onClick={() => setText(initialText)}>
            <img src={retryIcon} alt="" /><span>다시 변환</span>
          </button>
          <button type="button" className="transcription-action transcription-action--next" onClick={() => navigate('/reports/place')}>
            <span>다음</span><img src={nextIcon} alt="" />
          </button>
        </section>
      </main>

      <nav className="updated-mobile-nav" aria-label="주요 메뉴">
        <button type="button" onClick={() => navigate('/home')}><img src={navDelivery} alt="" /><span>Deliveries</span></button>
        <button type="button" className="active"><img src={navVoice} alt="" /><span>Voice<br />Tip</span></button>
        <button type="button"><img src={navNavigation} alt="" /><span>Navigation</span></button>
        <button type="button"><img src={navProfile} alt="" /><span>Profile</span></button>
      </nav>
    </div>
  )
}
