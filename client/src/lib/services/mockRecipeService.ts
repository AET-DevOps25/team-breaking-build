import { Recipe } from '@/lib/types/recipe';

const mockRecipes: Recipe[] = [
  {
    id: '1',
    userId: 1,
    createdAt: '2024-03-20T10:00:00Z',
    updatedAt: '2024-03-20T10:00:00Z',
    title: 'Classic Margherita Pizza',
    description: 'A simple yet delicious pizza with fresh tomatoes, mozzarella, and basil.',
    thumbnail: 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?q=80&w=1000',
    servingSize: 4,
    tags: ['Italian', 'Vegetarian', 'Quick'],
    ingredients: [
      { name: 'Pizza Dough', amount: 1, unit: 'ball' },
      { name: 'Fresh Mozzarella', amount: 200, unit: 'g' },
      { name: 'Fresh Basil', amount: 1, unit: 'bunch' },
      { name: 'Tomato Sauce', amount: 200, unit: 'ml' },
      { name: 'Olive Oil', amount: 2, unit: 'tbsp' },
    ],
    steps: [
      {
        order: 1,
        details: 'Preheat oven to 450°F (230°C) with a pizza stone inside.',
      },
      {
        order: 2,
        details: 'Roll out the pizza dough on a floured surface.',
      },
      {
        order: 3,
        details: 'Spread tomato sauce evenly over the dough.',
      },
      {
        order: 4,
        details: 'Add torn mozzarella pieces and fresh basil leaves.',
      },
      {
        order: 5,
        details: 'Drizzle with olive oil and bake for 12-15 minutes until golden.',
      },
    ],
  },
  {
    id: '2',
    userId: 1,
    createdAt: '2024-03-19T15:30:00Z',
    updatedAt: '2024-03-19T15:30:00Z',
    title: 'Chocolate Chip Cookies',
    description: 'Soft and chewy cookies with melty chocolate chips.',
    thumbnail: 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?q=80&w=1000',
    servingSize: 24,
    tags: ['Dessert', 'Baking', 'Sweet'],
    ingredients: [
      { name: 'Butter', amount: 200, unit: 'g' },
      { name: 'Brown Sugar', amount: 200, unit: 'g' },
      { name: 'White Sugar', amount: 100, unit: 'g' },
      { name: 'Eggs', amount: 2, unit: 'pieces' },
      { name: 'Vanilla Extract', amount: 2, unit: 'tsp' },
      { name: 'Flour', amount: 300, unit: 'g' },
      { name: 'Chocolate Chips', amount: 200, unit: 'g' },
    ],
    steps: [
      {
        order: 1,
        details: 'Cream together butter and sugars until light and fluffy.',
      },
      {
        order: 2,
        details: 'Beat in eggs one at a time, then stir in vanilla.',
      },
      {
        order: 3,
        details: 'Mix in flour and chocolate chips.',
      },
      {
        order: 4,
        details: 'Drop rounded tablespoons onto baking sheets.',
      },
      {
        order: 5,
        details: 'Bake at 350°F (175°C) for 10-12 minutes.',
      },
    ],
  },
  {
    id: '3',
    userId: 2,
    createdAt: '2024-03-18T09:15:00Z',
    updatedAt: '2024-03-18T09:15:00Z',
    title: 'Vegetable Stir Fry',
    description: 'Quick and healthy stir fry with seasonal vegetables.',
    thumbnail: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=1000',
    servingSize: 2,
    tags: ['Asian', 'Vegetarian', 'Healthy'],
    ingredients: [
      { name: 'Broccoli', amount: 1, unit: 'head' },
      { name: 'Carrots', amount: 2, unit: 'pieces' },
      { name: 'Bell Peppers', amount: 2, unit: 'pieces' },
      { name: 'Soy Sauce', amount: 3, unit: 'tbsp' },
      { name: 'Ginger', amount: 1, unit: 'tbsp' },
      { name: 'Garlic', amount: 2, unit: 'cloves' },
    ],
    steps: [
      {
        order: 1,
        details: 'Chop all vegetables into bite-sized pieces.',
      },
      {
        order: 2,
        details: 'Heat oil in a wok or large frying pan.',
      },
      {
        order: 3,
        details: 'Stir fry vegetables starting with the hardest ones.',
      },
      {
        order: 4,
        details: 'Add soy sauce and seasonings.',
      },
      {
        order: 5,
        details: 'Cook until vegetables are crisp-tender.',
      },
    ],
  },
];

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export async function getRecipes(_: number): Promise<Recipe[]> {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 500));
  return mockRecipes;
}

export async function getRecipe(id: string): Promise<Recipe | null> {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 500));
  return mockRecipes.find((recipe) => recipe.id === id) || null;
}

export async function createRecipe(data: FormData): Promise<Recipe> {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 1000));

  const newRecipe: Recipe = {
    id: (mockRecipes.length + 1).toString(),
    userId: 1, // Mock user ID
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    title: data.get('title') as string,
    description: data.get('description') as string,
    servingSize: parseInt(data.get('servingSize') as string),
    tags: data.getAll('tags') as string[],
    ingredients: JSON.parse(data.get('ingredients') as string),
    steps: JSON.parse(data.get('steps') as string),
  };

  // Handle thumbnail if present
  const thumbnail = data.get('thumbnail') as File | null;
  if (thumbnail) {
    // In a real app, we would upload this to a storage service
    // For mock purposes, we'll just use a placeholder
    newRecipe.thumbnail = 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?q=80&w=1000';
  }

  mockRecipes.push(newRecipe);
  return newRecipe;
}

export async function updateRecipe(id: string, data: FormData): Promise<Recipe | null> {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 1000));

  const index = mockRecipes.findIndex((recipe) => recipe.id === id);
  if (index === -1) return null;

  const updatedRecipe: Recipe = {
    ...mockRecipes[index],
    updatedAt: new Date().toISOString(),
    title: data.get('title') as string,
    description: data.get('description') as string,
    servingSize: parseInt(data.get('servingSize') as string),
    tags: data.getAll('tags') as string[],
    ingredients: JSON.parse(data.get('ingredients') as string),
    steps: JSON.parse(data.get('steps') as string),
  };

  // Handle thumbnail if present
  const thumbnail = data.get('thumbnail') as File | null;
  if (thumbnail) {
    // In a real app, we would upload this to a storage service
    // For mock purposes, we'll just use a placeholder
    updatedRecipe.thumbnail = 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?q=80&w=1000';
  }

  mockRecipes[index] = updatedRecipe;
  return updatedRecipe;
}

export async function deleteRecipe(id: string): Promise<void> {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 500));
  const index = mockRecipes.findIndex((recipe) => recipe.id === id);
  if (index !== -1) {
    mockRecipes.splice(index, 1);
  }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export async function copyRecipe(id: string, userId: number, versionId: number): Promise<Recipe | null> {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 1000));

  const originalRecipe = mockRecipes.find((recipe) => recipe.id === id);
  if (!originalRecipe) return null;

  const newRecipe: Recipe = {
    ...originalRecipe,
    id: (mockRecipes.length + 1).toString(),
    userId,
    forkedFrom: parseInt(id),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  mockRecipes.push(newRecipe);
  return newRecipe;
}
