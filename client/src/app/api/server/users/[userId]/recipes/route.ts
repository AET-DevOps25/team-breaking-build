import { NextRequest, NextResponse } from 'next/server';
import { serverApi } from '@/lib/server-api';

export async function GET(request: NextRequest, { params }: { params: Promise<{ userId: string }> }) {
  try {
    const { userId } = await params;
    const { searchParams } = new URL(request.url);
    const page = searchParams.get('page') || '0';
    const size = searchParams.get('size') || '10';
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    if (!accessToken) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const response = await serverApi.get(`/users/${userId}/recipes?page=${page}&size=${size}`, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error fetching user recipes:', error);
    return NextResponse.json({ error: 'Failed to fetch user recipes' }, { status: 500 });
  }
}
