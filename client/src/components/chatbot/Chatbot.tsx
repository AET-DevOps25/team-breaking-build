'use client'

import { useState, useRef, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'
import { MessageCircle, X, Send, ChefHat } from 'lucide-react'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'

import { useRouter } from 'next/navigation'

interface ChatMessage {
    id: string
    content: string
    isUser: boolean
    timestamp: Date
    recipe?: any
}

interface ChatResponse {
    reply: string
    sources?: string[]
    recipe_suggestion?: any
    recipe?: any
    timestamp: string
}

export function Chatbot() {
    const [isOpen, setIsOpen] = useState(false)
    const [messages, setMessages] = useState<ChatMessage[]>([
        {
            id: '1',
            content: 'Hello! I\'m your AI chef assistant. I can help you find recipes, create new ones, and answer cooking questions. What would you like to know?',
            isUser: false,
            timestamp: new Date()
        }
    ])
    const [inputValue, setInputValue] = useState('')
    const [isLoading, setIsLoading] = useState(false)
    const scrollAreaRef = useRef<HTMLDivElement>(null)
    const textareaRef = useRef<HTMLTextAreaElement>(null)
    const router = useRouter()

    // Scroll to bottom whenever messages change
    useEffect(() => {
        const scrollToBottom = () => {
            if (scrollAreaRef.current) {
                const scrollElement = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]')
                if (scrollElement) {
                    scrollElement.scrollTop = scrollElement.scrollHeight
                }
            }
        }

        // Use setTimeout to ensure DOM is updated
        setTimeout(scrollToBottom, 100)
    }, [messages, isLoading])

    useEffect(() => {
        if (isOpen && textareaRef.current) {
            textareaRef.current.focus()
        }
    }, [isOpen])

    // Auto-resize textarea
    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = '40px' // always start at 1 line (40px)
            const scrollHeight = textareaRef.current.scrollHeight
            const maxHeight = 32 * 8 // 8 lines * 32px per line (approximate)
            textareaRef.current.style.height = Math.min(Math.max(scrollHeight, 40), maxHeight) + 'px'
        }
    }, [inputValue])

    const sendMessage = async () => {
        if (!inputValue.trim() || isLoading) return

        const userMessage: ChatMessage = {
            id: Date.now().toString(),
            content: inputValue.trim(),
            isUser: true,
            timestamp: new Date()
        }

        setMessages(prev => [...prev, userMessage])
        setInputValue('')
        setIsLoading(true)

        try {
            const data: ChatResponse = await api.post<ChatResponse>('/genai/chat', {
                message: userMessage.content
            })

            const aiMessage: ChatMessage = {
                id: (Date.now() + 1).toString(),
                content: data.reply,
                isUser: false,
                timestamp: new Date(data.timestamp),
                recipe: data.recipe || data.recipe_suggestion
            }

            setMessages(prev => [...prev, aiMessage])
        } catch (error) {
            console.error('Error sending message:', error)
            const errorMessage: ChatMessage = {
                id: (Date.now() + 1).toString(),
                content: 'Sorry, I encountered an error. Please try again.',
                isUser: false,
                timestamp: new Date()
            }
            setMessages(prev => [...prev, errorMessage])
        } finally {
            setIsLoading(false)
        }
    }

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            sendMessage()
        }
    }

    const handleCreateRecipe = (recipe: any) => {
        // Remove any image fields from steps and thumbnail
        const { thumbnail, steps, ...rest } = recipe
        const stepsWithoutImages = Array.isArray(steps)
            ? steps.map(({ image, ...stepRest }: any) => stepRest)
            : []
        const prefillData = { ...rest, steps: stepsWithoutImages, thumbnail: undefined }
        // Store prefill data in sessionStorage for the create page to access
        sessionStorage.setItem('recipePrefillData', JSON.stringify(prefillData))
        router.push('/recipes/create')
    }

    return (
        <>
            {/* Floating Action Button - always fixed bottom right, never affected by chat dialog */}
            <div
                className={cn(
                    'fixed bottom-6 right-6 z-[9999] transition-all duration-500 ease-in-out',
                    isOpen ? 'opacity-0 pointer-events-none scale-95' : 'opacity-100 pointer-events-auto scale-100'
                )}
            >
                <Button
                    onClick={() => setIsOpen(true)}
                    className="h-16 w-16 rounded-full shadow-lg"
                    style={{ backgroundColor: '#FF7C75' }}
                >
                    <MessageCircle className="h-7 w-7" />
                </Button>
            </div>

            {/* Chat Dialog - always fixed bottom right, never affects FAB */}
            <div
                className={cn(
                    'fixed bottom-6 right-6 z-[10000] transition-all duration-500 ease-in-out',
                    isOpen ? 'opacity-100 pointer-events-auto scale-100' : 'opacity-0 pointer-events-none scale-95'
                )}
            >
                <Card className="w-[480px] h-[625px] shadow-xl border-0">
                    <CardContent className="p-0 h-full flex flex-col">
                        {/* Header */}
                        <div className="flex items-center justify-between p-4 border-b bg-gray-50 rounded-t-lg">
                            <div className="flex items-center gap-2">
                                <ChefHat className="h-5 w-5" style={{ color: '#FF7C75' }} />
                                <span className="font-semibold">AI Chef Assistant</span>
                            </div>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setIsOpen(false)}
                                className="h-8 w-8 p-0"
                            >
                                <X className="h-4 w-4" />
                            </Button>
                        </div>

                        {/* Messages */}
                        <ScrollArea className="flex-1 p-4" ref={scrollAreaRef}>
                            <div className="space-y-4">
                                {messages.map((message) => (
                                    <div
                                        key={message.id}
                                        className={cn(
                                            "flex",
                                            message.isUser ? "justify-end" : "justify-start"
                                        )}
                                    >
                                        <div
                                            className={cn(
                                                "max-w-[80%] rounded-lg px-3 py-2 text-sm",
                                                message.isUser
                                                    ? "text-white"
                                                    : "bg-gray-100 text-gray-900"
                                            )}
                                            style={message.isUser ? { backgroundColor: '#FF7C75' } : {}}
                                        >
                                            {message.content}

                                            {/* Recipe Creation Button */}
                                            {!message.isUser && message.recipe && (
                                                <div className="mt-3">
                                                    <Button
                                                        onClick={() => handleCreateRecipe(message.recipe)}
                                                        size="sm"
                                                        className="w-full flex items-center justify-center gap-2"
                                                        style={{ backgroundColor: '#FF7C75' }}
                                                    >
                                                        <ChefHat className="h-4 w-4" />
                                                        Let's Create
                                                    </Button>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                ))}

                                {/* Loading Indicator */}
                                {isLoading && (
                                    <div className="flex justify-start">
                                        <div className="bg-gray-100 rounded-lg px-3 py-2 text-sm">
                                            <div className="flex items-center gap-2">
                                                <div className="flex space-x-1">
                                                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                                                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }}></div>
                                                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                                                </div>
                                                <span className="text-gray-500">Chef is thinking...</span>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </ScrollArea>

                        {/* Input */}
                        <div className="p-4 border-t">
                            <div className="flex gap-2 items-end">
                                <div className="flex-1 relative">
                                    <textarea
                                        ref={textareaRef}
                                        value={inputValue}
                                        onChange={(e) => setInputValue(e.target.value)}
                                        onKeyPress={handleKeyPress}
                                        placeholder="Ask me about recipes..."
                                        disabled={isLoading}
                                        className="w-full resize-none border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 rounded-md overflow-y-auto"
                                        style={{
                                            lineHeight: '1.5',
                                            minHeight: '40px',
                                            maxHeight: '256px'
                                        }}
                                    />
                                </div>
                                <Button
                                    onClick={sendMessage}
                                    disabled={!inputValue.trim() || isLoading}
                                    size="sm"
                                    className="h-10 w-10 p-0 flex-shrink-0 py-2 mb-2"
                                    style={{ backgroundColor: '#FF7C75' }}
                                >
                                    <Send className="h-4 w-4" />
                                </Button>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </>
    )
} 