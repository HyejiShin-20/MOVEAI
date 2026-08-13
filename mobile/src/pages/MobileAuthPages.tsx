import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { routes } from '../routes'

const authAssets = {
  back: 'https://www.figma.com/api/mcp/asset/49f41a89-1839-4b21-9956-67b81f3eb5de.svg',
  backInfo: 'https://www.figma.com/api/mcp/asset/cb38b37f-9cbc-4ed4-8fbb-15a620587ba9.svg',
  logo: 'https://www.figma.com/api/mcp/asset/bfdc5ced-0bc8-4f55-bc48-4f054d6e23ae.png',
  user: 'https://www.figma.com/api/mcp/asset/e8de07c1-ac4e-40c0-93c7-a79d48c12084.svg',
  lock: 'https://www.figma.com/api/mcp/asset/7583f549-dbee-4597-a675-2e5216f91555.svg',
  kakao: 'https://www.figma.com/api/mcp/asset/abadb169-2d7e-4b9a-a5eb-2a5892206326.svg',
  search: 'https://www.figma.com/api/mcp/asset/c7cba503-161d-4d0d-9ea8-a043ec2f1850.svg',
  eye: 'https://www.figma.com/api/mcp/asset/50951fca-61b5-4564-a2ad-3d606c75da2f.svg',
} as const

function MobileSignupHeader({ onBack }: { onBack: () => void }) {
  return (
    <header className="mobile-signup-header">
      <button type="button" className="mobile-signup-header__back" aria-label="뒤로 가기" onClick={onBack}>
        <img src={authAssets.back} alt="" />
      </button>
      <h1>회원가입</h1>
      <span className="mobile-signup-header__spacer" aria-hidden="true" />
    </header>
  )
}

export function MobileLoginPage() {
  const navigate = useNavigate()
  const [remember, setRemember] = useState(false)

  const submitLogin = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    navigate(routes.home)
  }

  return (
    <main className="mobile-auth-screen mobile-login-screen" aria-labelledby="mobile-login-title">
      <div className="mobile-login-main">
        <div className="mobile-login-brand">
          <div className="mobile-login-logo" aria-hidden="true">
            <img src={authAssets.logo} alt="" />
          </div>
          <p id="mobile-login-title">Driver Operations Portal</p>
        </div>

        <form className="mobile-login-form" onSubmit={submitLogin}>
          <div className="mobile-login-fields">
            <label className="mobile-login-input">
              <img src={authAssets.user} alt="" />
              <input type="text" name="id" placeholder="ID" autoComplete="username" />
            </label>
            <label className="mobile-login-input">
              <img className="mobile-login-input__lock" src={authAssets.lock} alt="" />
              <input type="password" name="password" placeholder="비밀번호" autoComplete="current-password" />
            </label>
            <div className="mobile-login-utils">
              <label className="mobile-checkbox-label">
                <input type="checkbox" checked={remember} onChange={(event) => setRemember(event.target.checked)} />
                <span className="mobile-checkbox-box" aria-hidden="true" />
                <span>자동 로그인</span>
              </label>
              <button type="button" className="mobile-auth-text-button">비밀번호 찾기</button>
            </div>
          </div>

          <div className="mobile-login-actions">
            <button type="submit" className="mobile-login-button mobile-login-button--outline">로그인</button>
            <button type="button" className="mobile-login-button mobile-login-button--kakao" onClick={() => navigate(routes.home)}>
              <img src={authAssets.kakao} alt="" />
              <span>카카오로 로그인</span>
            </button>
          </div>
        </form>

        <p className="mobile-login-footer">
          <span>아직 회원이 아니신가요?</span>
          <button type="button" onClick={() => navigate(routes.signup)}>회원가입</button>
        </p>
      </div>
    </main>
  )
}

export function MobileSignupTermsPage() {
  const navigate = useNavigate()
  const [terms, setTerms] = useState({ service: false, privacy: false, marketing: false })
  const allChecked = terms.service && terms.privacy && terms.marketing

  const setAll = (checked: boolean) => {
    setTerms({ service: checked, privacy: checked, marketing: checked })
  }

  const setTerm = (key: keyof typeof terms, checked: boolean) => {
    setTerms((current) => ({ ...current, [key]: checked }))
  }

  return (
    <main className="mobile-auth-screen mobile-signup-screen mobile-signup-terms" aria-labelledby="mobile-signup-terms-title">
      <MobileSignupHeader onBack={() => navigate(routes.login)} />

      <section className="mobile-signup-progress" aria-label="회원가입 진행 단계">
        <div className="mobile-signup-progress__copy">
          <strong id="mobile-signup-terms-title">약관동의</strong>
          <span>2중 1단계</span>
        </div>
        <div className="mobile-signup-progress__bar" />
      </section>

      <section className="mobile-signup-terms__content">
        <p className="mobile-signup-description">아래 약관을 읽고 동의해주세요</p>
        <div className="mobile-terms-card">
          <label className="mobile-terms-all">
            <input type="checkbox" checked={allChecked} onChange={(event) => setAll(event.target.checked)} />
            <span className="mobile-terms-check" aria-hidden="true" />
            <strong>Agree to all terms</strong>
          </label>

          <div className="mobile-terms-list">
            <label className="mobile-term-row">
              <input type="checkbox" checked={terms.service} onChange={(event) => setTerm('service', event.target.checked)} />
              <span className="mobile-terms-check" aria-hidden="true" />
              <span className="mobile-term-copy"><strong>[필수] 이용약관 <em>*</em></strong><button type="button">Read full text</button></span>
            </label>
            <label className="mobile-term-row">
              <input type="checkbox" checked={terms.privacy} onChange={(event) => setTerm('privacy', event.target.checked)} />
              <span className="mobile-terms-check" aria-hidden="true" />
              <span className="mobile-term-copy"><strong>[필수] 정보처리방침 <em>*</em></strong><button type="button">Read full text</button></span>
            </label>
            <label className="mobile-term-row">
              <input type="checkbox" checked={terms.marketing} onChange={(event) => setTerm('marketing', event.target.checked)} />
              <span className="mobile-terms-check" aria-hidden="true" />
              <span className="mobile-term-copy"><strong>[선택] 마케팅 동의</strong><small>업데이트/혜택 등 알림 수신</small></span>
            </label>
          </div>
        </div>
      </section>

      <button type="button" className="mobile-signup-next" disabled={!terms.service || !terms.privacy} onClick={() => navigate(routes.signupInfo)}>다음</button>
    </main>
  )
}

export function MobileSignupInfoPage() {
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const submitInfo = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    navigate(routes.login)
  }

  return (
    <main className="mobile-auth-screen mobile-signup-screen mobile-signup-info" aria-labelledby="mobile-signup-info-title">
      <MobileSignupHeader onBack={() => navigate(routes.signup)} />

      <form className="mobile-signup-info__main" onSubmit={submitInfo}>
        <section className="mobile-signup-progress mobile-signup-progress--complete" aria-label="회원가입 진행 단계">
          <div className="mobile-signup-progress__copy">
            <strong id="mobile-signup-info-title">회원정보</strong>
            <span>2중 2단계</span>
          </div>
          <div className="mobile-signup-progress__bar"><span /></div>
        </section>

        <div className="mobile-signup-info-fields">
          <MobileInfoField label="이름" required placeholder="홍길동" name="name" autoComplete="name" />
          <MobileInfoField label="ID/Email" required placeholder="주소" name="email" type="email" autoComplete="email" actionIcon={authAssets.search} actionLabel="ID/Email 검색" />
          <MobileInfoField label="주소" required placeholder="주소" name="address" autoComplete="street-address" actionIcon={authAssets.search} actionLabel="주소 검색" />
          <MobileInfoField label="소속기관" required placeholder="주소" name="organization" />
          <MobileInfoField label="연락처" required placeholder="010-0000-0000" name="phone" type="tel" autoComplete="tel" trailingButton="인증" />
          <MobileInfoField
            label="비밀번호"
            required
            placeholder="8자 이상, 대소문자 및 숫자 모두 포함"
            name="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            actionIcon={authAssets.eye}
            actionLabel={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
            onAction={() => setShowPassword((value) => !value)}
          />
          <MobileInfoField
            label="비밀번호 확인"
            required
            placeholder=""
            name="passwordConfirm"
            type={showConfirmPassword ? 'text' : 'password'}
            autoComplete="new-password"
            actionIcon={authAssets.eye}
            actionLabel={showConfirmPassword ? '비밀번호 확인 숨기기' : '비밀번호 확인 보기'}
            onAction={() => setShowConfirmPassword((value) => !value)}
          />
        </div>

        <button type="submit" className="mobile-signup-next mobile-signup-next--form">다음</button>
      </form>
    </main>
  )
}

type MobileInfoFieldProps = {
  label: string
  required?: boolean
  placeholder: string
  name: string
  type?: string
  autoComplete?: string
  actionIcon?: string
  actionLabel?: string
  onAction?: () => void
  trailingButton?: string
}

function MobileInfoField({
  label,
  required,
  placeholder,
  name,
  type = 'text',
  autoComplete,
  actionIcon,
  actionLabel,
  onAction,
  trailingButton,
}: MobileInfoFieldProps) {
  return (
    <label className="mobile-info-field">
      <span className="mobile-info-field__label">{label}{required && <em>*</em>}</span>
      <span className="mobile-info-input">
        <input type={type} name={name} placeholder={placeholder} autoComplete={autoComplete} />
        {actionIcon && (
          <button type="button" className="mobile-info-input__icon" aria-label={actionLabel} onClick={onAction}>
            <img src={actionIcon} alt="" />
          </button>
        )}
        {trailingButton && <button type="button" className="mobile-info-input__verify">{trailingButton}</button>}
      </span>
    </label>
  )
}
