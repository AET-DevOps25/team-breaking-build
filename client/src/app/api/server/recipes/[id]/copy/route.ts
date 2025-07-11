import { NextRequest, NextResponse } from 'next/server';
import { serverApi } from '@/lib/server-api';

export async function POST(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    const { searchParams } = new URL(request.url);
    const branchId = searchParams.get('branchId');
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    if (!accessToken) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    if (!branchId) {
      return NextResponse.json({ error: 'Branch ID is required' }, { status: 400 });
    }

    const response = await serverApi.post(`/recipes/${id}/copy?branchId=${branchId}`, undefined, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error copying recipe:', error);
    return NextResponse.json({ error: 'Failed to copy recipe' }, { status: 500 });
  }
}
