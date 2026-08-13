import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'

const signupAssets = {
  background: 'https://www.figma.com/api/mcp/asset/403880da-fa60-4d88-b2fb-eedd381bc72f.png',
  check: 'https://www.figma.com/api/mcp/asset/b50c0936-d061-469d-a07e-4c995e09cec0.svg',
  eye: 'https://www.figma.com/api/mcp/asset/5a633613-c679-4f21-8eb5-c4c2f3e9509e.svg',
  company: 'https://www.figma.com/api/mcp/asset/a3879a0c-05b6-4596-abfa-40a2d942d3da.svg',
  select: 'https://www.figma.com/api/mcp/asset/e148aa72-26e8-4d24-a38b-8873ccdd2654.svg',
  chevron: 'https://www.figma.com/api/mcp/asset/dca031f9-977d-428f-b1af-6dd4a47da4f2.svg',
  arrow: 'https://www.figma.com/api/mcp/asset/28c23d20-6365-4079-ac12-70703c6cb63b.svg',
} as const

export function WebSignupPage() {
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    navigate('/login')
  }

  return (
    <main className="web-signup-page" aria-labelledby="web-signup-title">
      <aside className="web-signup-visual">
        <img src={signupAssets.background} alt="" />
        <div className="web-signup-visual__overlay" />
        <div className="web-signup-visual__brand">
          <strong>Kakao Logistics</strong>
          <span>Utility Driver Network</span>
        </div>
        <div className="web-signup-visual__copy">
          <h2>효율적인 배송의 시작</h2>
          <p>카카오 로지스틱스 기사님으로 등록하시고, 최적화된 경로와 함께 더 스마트한 배송을 경험하세요.</p>
        </div>
      </aside>

      <section className="web-signup-panel">
        <div className="web-signup-container">
          <header className="web-signup-header">
            <h1 id="web-signup-title">기사님 정보 입력</h1>
            <div className="signup-steps" aria-label="가입 단계">
              <div className="signup-step signup-step--done">
                <span className="signup-step__circle"><img src={signupAssets.check} alt="" /></span>
                <span>약관 동의</span>
              </div>
              <span className="signup-step__line" />
              <div className="signup-step signup-step--active">
                <span className="signup-step__circle">2</span>
                <span>정보 입력</span>
              </div>
            </div>
          </header>

          <form className="signup-form" onSubmit={handleSubmit}>
            <section className="signup-form-section signup-form-section--bordered">
              <h2>계정 정보</h2>
              <label className="signup-field signup-field--full">
                <span>아이디 (이메일) <em>*</em></span>
                <span className="signup-field__row">
                  <input type="email" placeholder="example@kakaologistics.com" autoComplete="email" />
                  <button className="signup-secondary-button" type="button">중복확인</button>
                </span>
              </label>
              <div className="signup-grid signup-grid--two">
                <label className="signup-field">
                  <span>비밀번호 <em>*</em></span>
                  <span className="signup-input-action">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      placeholder="영문, 숫자, 특수문자 조합 8자 이상"
                      autoComplete="new-password"
                    />
                    <button type="button" onClick={() => setShowPassword((value) => !value)} aria-label="비밀번호 보기">
                      <img src={signupAssets.eye} alt="" />
                    </button>
                  </span>
                </label>
                <label className="signup-field">
                  <span>비밀번호 확인 <em>*</em></span>
                  <input type="password" placeholder="비밀번호 재입력" autoComplete="new-password" />
                </label>
              </div>
            </section>

            <section className="signup-form-section">
              <h2>개인 및 소속 정보</h2>
              <div className="signup-grid signup-grid--two">
                <label className="signup-field">
                  <span>이름 <em>*</em></span>
                  <input type="text" placeholder="실명 입력" autoComplete="name" />
                </label>
                <label className="signup-field">
                  <span>소속 <em>*</em></span>
                  <span className="signup-input-icon">
                    <img src={signupAssets.company} alt="" />
                    <input type="text" placeholder="지점 또는 협력사명" />
                  </span>
                </label>
              </div>

              <label className="signup-field signup-field--full">
                <span>휴대전화 <em>*</em></span>
                <span className="signup-field__row">
                  <input type="tel" placeholder="'-' 제외하고 입력" autoComplete="tel" />
                  <button className="signup-secondary-button" type="button">인증요청</button>
                </span>
              </label>

              <label className="signup-field signup-field--full">
                <span>주소 <em>*</em></span>
                <span className="signup-address-row">
                  <input className="signup-postcode" type="text" placeholder="우편번호" />
                  <button className="signup-secondary-button" type="button">주소 검색</button>
                </span>
                <input type="text" placeholder="기본 주소" />
                <input type="text" placeholder="상세 주소 입력" />
              </label>

              <label className="signup-field signup-field--full">
                <span>배송 경력 <small>(선택)</small></span>
                <span className="signup-select-wrap">
                  <select defaultValue="">
                    <option value="" disabled>경력을 선택해주세요</option>
                    <option value="under-1">1년 미만</option>
                    <option value="1-3">1~3년</option>
                    <option value="3-5">3~5년</option>
                    <option value="5-plus">5년 이상</option>
                  </select>
                  <img className="signup-select-wrap__source" src={signupAssets.select} alt="" aria-hidden="true" />
                  <img className="signup-select-wrap__chevron" src={signupAssets.chevron} alt="" aria-hidden="true" />
                </span>
              </label>
            </section>

            <button className="auth-primary-button signup-submit" type="submit">
              <span>가입 완료</span>
              <img src={signupAssets.arrow} alt="" />
            </button>
          </form>
        </div>
      </section>
    </main>
  )
}
