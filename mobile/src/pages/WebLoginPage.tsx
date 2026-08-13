import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'

const loginAssets = {
  background: 'https://www.figma.com/api/mcp/asset/f16596eb-943c-4bfb-9a07-2bf735ef95d4.png',
  brand: 'https://www.figma.com/api/mcp/asset/841d11e4-ffb0-485f-b6a5-8d16dec71012.svg',
  id: 'https://www.figma.com/api/mcp/asset/2ee95038-7d57-46eb-8ebb-af526933677e.svg',
  password: 'https://www.figma.com/api/mcp/asset/5d7fccc8-12d2-4b32-b6a6-2bfaf4ea97aa.svg',
  eye: 'https://www.figma.com/api/mcp/asset/293e3743-76fa-4c75-b66d-53edd5f87260.svg',
  arrow: 'https://www.figma.com/api/mcp/asset/703d101b-c961-4c99-baa6-be938f6c3598.svg',
} as const

export function WebLoginPage() {
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [adminLogin, setAdminLogin] = useState(true)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    navigate(adminLogin ? '/admin' : '/home')
  }

  return (
    <main className="web-auth-page web-login-page">
      <section className="web-login-card" aria-labelledby="web-login-title">
        <div className="web-login-visual">
          <img className="web-login-visual__image" src={loginAssets.background} alt="" />
          <div className="web-login-visual__overlay" />
          <div className="web-login-visual__copy">
            <div className="web-login-brand">
              <img src={loginAssets.brand} alt="" />
              <span>Logistics Pro</span>
            </div>
            <h2>Operations Control</h2>
            <p>Streamline your delivery fleet, manage routes, and ensure<br />real-time visibility across all logistics nodes.</p>
          </div>
        </div>

        <div className="web-login-form-panel">
          <div className="web-login-form-wrap">
            <header className="web-login-heading">
              <h1 id="web-login-title">Welcome Back</h1>
              <p>Sign in to access the administrator dashboard.</p>
            </header>

            <form className="web-login-form" onSubmit={handleSubmit}>
              <label className="auth-field">
                <span>Employee ID / Email</span>
                <span className="auth-input auth-input--icon">
                  <img src={loginAssets.id} alt="" />
                  <input type="email" name="email" placeholder="Enter your ID" autoComplete="username" />
                </span>
              </label>

              <label className="auth-field">
                <span className="auth-field__label-row">
                  <span>Password</span>
                  <button className="auth-text-button" type="button">Forgot password?</button>
                </span>
                <span className="auth-input auth-input--icon auth-input--with-action">
                  <img src={loginAssets.password} alt="" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    placeholder="Enter your password"
                    autoComplete="current-password"
                  />
                  <button
                    className="auth-input__action"
                    type="button"
                    aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                    onClick={() => setShowPassword((value) => !value)}
                  >
                    <img src={loginAssets.eye} alt="" />
                  </button>
                </span>
              </label>

              <label className="admin-toggle-row">
                <input
                  type="checkbox"
                  checked={adminLogin}
                  onChange={(event) => setAdminLogin(event.target.checked)}
                />
                <span className="admin-toggle" aria-hidden="true"><span /></span>
                <span>Admin Login</span>
              </label>

              <button className="auth-primary-button auth-primary-button--large" type="submit">
                <span>Sign In</span>
                <img src={loginAssets.arrow} alt="" />
              </button>
            </form>

            <p className="web-login-support">
              Need help accessing your account? <a href="mailto:support@example.com">Contact IT Support</a>
            </p>

          </div>
        </div>
      </section>
    </main>
  )
}
