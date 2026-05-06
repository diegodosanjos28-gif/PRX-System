import api from './axios';
import { LoginRequest, JwtResponse } from '@/lib/types/api';

export const login = (data: LoginRequest) =>
  api.post<JwtResponse>('/api/auth/login', data).then((r) => r.data);
