import { Routes, Route } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AuthProvider } from '@/contexts/AuthContext'
import { Chatbot } from '@/components/chatbot/Chatbot'
import NavBar from '@/components/navigation/NavBar'

// Import page components
import HomePage from '@/pages/HomePage'
import LoginPage from '@/pages/LoginPage'
import RegisterPage from '@/pages/RegisterPage'
import ProfilePage from '@/pages/ProfilePage'
import UserProfilePage from '@/pages/UserProfilePage'
import RecipesPage from '@/pages/RecipesPage'
import CreateRecipePage from '@/pages/CreateRecipePage'
import RecipeDetailPage from '@/pages/RecipeDetailPage'
import EditRecipePage from '@/pages/EditRecipePage'
import RecipeHistoryPage from '@/pages/RecipeHistoryPage'
import RecipeChangesPage from '@/pages/RecipeChangesPage'
import NotFoundPage from '@/pages/NotFoundPage'

function App() {
    return (
        <div className="min-h-screen bg-[#fafafa] font-inter antialiased">
            <AuthProvider>
                <NavBar />
                <main>
                    <Routes>
                        <Route path="/" element={<HomePage />} />
                        <Route path="/login" element={<LoginPage />} />
                        <Route path="/register" element={<RegisterPage />} />
                        <Route path="/profile" element={<ProfilePage />} />
                        <Route path="/users/:userId" element={<UserProfilePage />} />
                        <Route path="/recipes" element={<RecipesPage />} />
                        <Route path="/recipes/create" element={<CreateRecipePage />} />
                        <Route path="/recipes/:id" element={<RecipeDetailPage />} />
                        <Route path="/recipes/:id/edit" element={<EditRecipePage />} />
                        <Route path="/recipes/:id/history" element={<RecipeHistoryPage />} />
                        <Route path="/recipes/:id/changes/:commitId" element={<RecipeChangesPage />} />
                        <Route path="*" element={<NotFoundPage />} />
                    </Routes>
                </main>
                <Toaster />
                <Chatbot />
            </AuthProvider>
        </div>
    )
}

export default App 