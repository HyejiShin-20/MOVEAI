import { Navigate, Route, Routes } from 'react-router-dom'
import { HomePage } from './pages/HomePage'
import { LoginRoutePage, SignupInfoRoutePage, SignupRoutePage } from './pages/AuthRoutePages'
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
import { AdminDashboardPage } from './pages/AdminDashboardPage'
import { AdminReviewListPage } from './pages/AdminReviewListPage'
import { AdminReviewDetailPage } from './pages/AdminReviewDetailPage'
import { AdminPlaceManagementPage } from './pages/AdminPlaceManagementPage'
import { AdminRouteEditPage } from './pages/AdminRouteEditPage'
import { AdminRouteKnowledgePage } from './pages/AdminRouteKnowledgePage'
import { AdminRouteValidationPage } from './pages/AdminRouteValidationPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginRoutePage />} />
      <Route path="/signup" element={<SignupRoutePage />} />
      <Route path="/signup/info" element={<SignupInfoRoutePage />} />
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
      <Route path="/admin" element={<AdminDashboardPage />} />
      <Route path="/admin/reviews" element={<AdminReviewListPage />} />
      <Route path="/admin/reviews/:reportId" element={<AdminReviewDetailPage />} />
      <Route path="/admin/places" element={<AdminPlaceManagementPage />} />
      <Route path="/admin/routes" element={<AdminRouteEditPage />} />
      <Route path="/admin/routes/knowledge" element={<AdminRouteKnowledgePage />} />
      <Route path="/admin/routes/validation" element={<AdminRouteValidationPage />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
