import { Recipe, RecipeResponse } from '@/lib/types/recipe';

const mockRecipes: Recipe[] = [
  {
    id: 1,
    userId: 1,
    title: 'Classic Margherita Pizza',
    description: 'A simple yet delicious pizza with fresh tomatoes, mozzarella, and basil',
    thumbnail: {
      id: 1,
      url: 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3',
    },
    servingSize: 4,
    tags: [
      { id: 1, name: 'Italian' },
      { id: 2, name: 'Vegetarian' },
    ],
    createdAt: '2024-03-20T10:00:00Z',
    updatedAt: '2024-03-20T10:00:00Z',
  },
  {
    id: 2,
    userId: 1,
    title: 'Chocolate Chip Cookies',
    description: 'Soft and chewy cookies with chocolate chips',
    thumbnail: {
      id: 2,
      url: 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e',
    },
    servingSize: 24,
    tags: [
      { id: 3, name: 'Dessert' },
      { id: 4, name: 'Baking' },
    ],
    createdAt: '2024-03-19T15:30:00Z',
    updatedAt: '2024-03-19T15:30:00Z',
  },
  {
    id: 3,
    userId: 2,
    title: 'Beef Wellington',
    description: 'Classic British dish with beef fillet wrapped in puff pastry',
    thumbnail: {
      id: 3,
      url: 'https://images.unsplash.com/photo-1600891964092-4316c288032e',
    },
    servingSize: 6,
    tags: [
      { id: 5, name: 'British' },
      { id: 6, name: 'Meat' },
    ],
    createdAt: '2024-03-18T12:00:00Z',
    updatedAt: '2024-03-18T12:00:00Z',
  },
];

export const getRecipes = async (page: number = 1, pageSize: number = 6): Promise<RecipeResponse> => {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 500));

  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  const paginatedRecipes = mockRecipes.slice(start, end);

  return {
    recipes: paginatedRecipes,
    total: mockRecipes.length,
    page,
    pageSize,
  };
};
