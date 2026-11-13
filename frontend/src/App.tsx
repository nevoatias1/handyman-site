import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Home from './pages/Home'
import AdminShell from './components/AdminShell'
import ProtectedRoute from './components/ProtectedRoute'
import AdminLogin from './pages/AdminLogin'
import AdminDashboard from './pages/AdminDashboard'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="admin" element={<AdminShell />}>
          <Route path="login" element={<AdminLogin />} />
          <Route element={<ProtectedRoute />}>
            <Route index element={<AdminDashboard />} />
          </Route>
        </Route>
        <Route element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="*" element={<p>העמוד לא נמצא.</p>} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
