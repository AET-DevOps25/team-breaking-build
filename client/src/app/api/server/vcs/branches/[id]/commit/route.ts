import { NextRequest, NextResponse } from 'next/server';
import { serverApi } from '@/lib/server-api';

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    const body = await request.json();
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    if (!accessToken) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const response = await serverApi.post(`/vcs/branches/${id}/commit`, body, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error creating commit:', error);
    return NextResponse.json({ error: 'Failed to create commit' }, { status: 500 });
  }
}
