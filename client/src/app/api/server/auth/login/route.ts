import { NextRequest, NextResponse } from 'next/server';
import { serverAuth } from '@/lib/server-api';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    const response = await serverAuth.post('/login', body);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error in login:', error);
    return NextResponse.json({ error: 'Login failed' }, { status: 401 });
  }
}
