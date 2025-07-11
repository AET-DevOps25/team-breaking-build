import { NextRequest, NextResponse } from 'next/server';
import { serverApi } from '@/lib/server-api';

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const page = searchParams.get('page') || '0';
    const size = searchParams.get('size') || '10';
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    const response = await serverApi.get(`/recipes?page=${page}&size=${size}`, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error fetching recipes:', error);
    return NextResponse.json({ error: 'Failed to fetch recipes' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const accessToken = request.headers.get('authorization')?.replace('Bearer ', '');

    if (!accessToken) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const response = await serverApi.post('/recipes', body, accessToken);
    return NextResponse.json(response);
  } catch (error) {
    console.error('Error creating recipe:', error);
    return NextResponse.json({ error: 'Failed to create recipe' }, { status: 500 });
  }
}
