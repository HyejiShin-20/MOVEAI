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
import { routes } from './routes'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to={routes.login} replace />} />
      <Route path={routes.login} element={<LoginRoutePage />} />
      <Route path={routes.signup} element={<SignupRoutePage />} />
      <Route path={routes.signupInfo} element={<SignupInfoRoutePage />} />
      <Route path={routes.home} element={<HomePage />} />
      <Route path={routes.reportRecord} element={<VoiceRecordPage />} />
      <Route path={routes.reportUploading} element={<VoiceUploadPage />} />
      <Route path={routes.reportTranscription} element={<TranscriptionPage />} />
      <Route path={routes.reportPlace} element={<PlaceSelectPage />} />
      <Route path={routes.reportConfirm} element={<ReportConfirmPage />} />
      <Route path={routes.reportDrafts} element={<DraftReportsPage />} />
      <Route path={routes.myReports} element={<MyReportsPage />} />
      <Route path={routes.guidancePreview} element={<GuidancePreviewPage />} />
      <Route path={routes.guidanceStep} element={<GuidanceStepPage />} />
      <Route path={routes.guidanceCompleted} element={<GuidanceCompletedPage />} />
      <Route path={routes.guidanceUnavailable} element={<GuidanceUnavailablePage />} />
      <Route path={routes.admin} element={<AdminDashboardPage />} />
      <Route path={routes.adminReviews} element={<AdminReviewListPage />} />
      <Route path="/admin/reviews/:reportId" element={<AdminReviewDetailPage />} />
      <Route path={routes.adminPlaces} element={<AdminPlaceManagementPage />} />
      <Route path={routes.adminRoutes} element={<AdminRouteEditPage />} />
      <Route path={routes.adminKnowledge} element={<AdminRouteKnowledgePage />} />
      <Route path={routes.adminValidation} element={<AdminRouteValidationPage />} />
      <Route path="*" element={<Navigate to={routes.login} replace />} />
    </Routes>
  )
}
