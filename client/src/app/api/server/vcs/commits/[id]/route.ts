import { NextRequest, NextResponse } from 'next/server';
import { serverApi } from '@/lib/server-api';

export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    const response = await serverApi.get(`/vcs/commits/${id}`, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error fetching commit details:', error);
    return NextResponse.json({ error: 'Failed to fetch commit details' }, { status: 500 });
  }
}
