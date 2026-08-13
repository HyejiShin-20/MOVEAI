import { WebLoginPage } from './WebLoginPage'
import { WebSignupPage } from './WebSignupPage'
import { MobileLoginPage, MobileSignupInfoPage, MobileSignupTermsPage } from './MobileAuthPages'

export function LoginRoutePage() {
  return (
    <>
      <div className="desktop-auth-route"><WebLoginPage /></div>
      <div className="mobile-auth-route"><MobileLoginPage /></div>
    </>
  )
}

export function SignupRoutePage() {
  return (
    <>
      <div className="desktop-auth-route"><WebSignupPage /></div>
      <div className="mobile-auth-route"><MobileSignupTermsPage /></div>
    </>
  )
}

export function SignupInfoRoutePage() {
  return (
    <>
      <div className="desktop-auth-route"><WebSignupPage /></div>
      <div className="mobile-auth-route"><MobileSignupInfoPage /></div>
    </>
  )
}
