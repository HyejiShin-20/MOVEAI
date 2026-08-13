import { Navigate, Route, Routes } from 'react-router-dom'
import { HomePage } from './pages/HomePage'
import { WebLoginPage } from './pages/WebLoginPage'
import { WebSignupPage } from './pages/WebSignupPage'
import { VoiceRecordPage } from './pages/VoiceRecordPage'
import { VoiceUploadPage } from './pages/VoiceUploadPage'
import { TranscriptionPage } from './pages/TranscriptionPage'
import { PlaceSelectPage } from './pages/PlaceSelectPage'
import { ReportConfirmPage } from './pages/ReportConfirmPage'
import { DraftReportsPage } from './pages/DraftReportsPage'
import { MyReportsPage } from './pages/MyReportsPage'
import { GuidancePreviewPage } from './pages/GuidancePreviewPage'
import { GuidanceStepPage } from './pages/GuidanceStepPage'
import { GuidanceCompletedPage } from './pages/GuidanceCompletedPage'
import { GuidanceUnavailablePage } from './pages/GuidanceUnavailablePage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<WebLoginPage />} />
      <Route path="/signup" element={<WebSignupPage />} />
      <Route path="/home" element={<HomePage />} />
      <Route path="/reports/record" element={<VoiceRecordPage />} />
      <Route path="/reports/uploading" element={<VoiceUploadPage />} />
      <Route path="/reports/transcription" element={<TranscriptionPage />} />
      <Route path="/reports/place" element={<PlaceSelectPage />} />
      <Route path="/reports/confirm" element={<ReportConfirmPage />} />
      <Route path="/reports/drafts" element={<DraftReportsPage />} />
      <Route path="/reports/mine" element={<MyReportsPage />} />
      <Route path="/guidance/preview" element={<GuidancePreviewPage />} />
      <Route path="/guidance/step" element={<GuidanceStepPage />} />
      <Route path="/guidance/completed" element={<GuidanceCompletedPage />} />
      <Route path="/guidance/unavailable" element={<GuidanceUnavailablePage />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
