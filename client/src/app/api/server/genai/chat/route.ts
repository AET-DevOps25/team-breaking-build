import { NextRequest, NextResponse } from 'next/server';
import { serverApi } from '@/lib/server-api';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    const response = await serverApi.post('/genai/chat', body, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error in GenAI chat:', error);
    return NextResponse.json({ error: 'Failed to process chat request' }, { status: 500 });
  }
}
