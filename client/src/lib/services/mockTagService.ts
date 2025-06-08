export interface Tag {
  id: number;
  name: string;
}

const mockTags: Tag[] = [
  { id: 1, name: 'Breakfast' },
  { id: 2, name: 'Lunch' },
  { id: 3, name: 'Dinner' },
  { id: 4, name: 'Dessert' },
  { id: 5, name: 'Vegetarian' },
  { id: 6, name: 'Vegan' },
  { id: 7, name: 'Gluten-Free' },
  { id: 8, name: 'Quick & Easy' },
  { id: 9, name: 'Healthy' },
  { id: 10, name: 'Italian' },
  { id: 11, name: 'Mexican' },
  { id: 12, name: 'Asian' },
  { id: 13, name: 'Mediterranean' },
  { id: 14, name: 'Low Carb' },
  { id: 15, name: 'High Protein' },
];

export const getTags = async (): Promise<Tag[]> => {
  // Simulate API delay
  await new Promise((resolve) => setTimeout(resolve, 500));
  return mockTags;
};
