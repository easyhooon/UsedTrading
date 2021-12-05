package kr.ac.konkuk.usedtrading.mypage

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.usedtrading.R
import kr.ac.konkuk.usedtrading.databinding.FragmentMyPageBinding

class MyPageFragment : Fragment(R.layout.fragment_my_page) {

    private var binding: FragmentMyPageBinding? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentMyPageBinding = FragmentMyPageBinding.bind(view)

        binding = fragmentMyPageBinding

        fragmentMyPageBinding.signInOutButton.setOnClickListener {
            binding?.let {binding->
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()

                if(auth.currentUser == null) {
                    //로그인
                    auth.signInWithEmailAndPassword(email, password)
                        //activity가 들어갈 자리가 nullable이기 때문에 requireActivity를 사용
                        //requireActivity를 사용할때 조심해야할 점은 activity가 null이면 exception을 발생시켜서 앱이 죽음
                        //activity는 절대 null이 아니고 fragment가 activity에서 떨어지지 않는다는 보장이 필요
                        .addOnCompleteListener(requireActivity()) { task->
                            if(task.isSuccessful) {
                                successSignIn()
                            } else {
                                Toast.makeText(context, "로그인에 실패했습니다, 이메일 또는 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                            }

                        }

                } else {
                    auth.signOut()
                    binding.emailEditText.text.clear()
                    binding.emailEditText.isEnabled = true

                    binding.passwordEditText.text.clear()
                    binding.passwordEditText.isEnabled = true

                    binding.signInOutButton.text = "로그인"
                    binding.signInOutButton.isEnabled = false
                    binding.signUpButton.isEnabled = false
                }
            }
        }

        fragmentMyPageBinding.signUpButton.setOnClickListener {
            binding?.let {binding->
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) {task ->
                        if(task.isSuccessful) {
                            Toast.makeText(context, "회원가입에 성공했습니다. 로그인 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "회원가입에 실패했습니다 이미 가입한 이메일일 수 있습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        fragmentMyPageBinding.emailEditText.addTextChangedListener{
            binding?.let { binding ->
                val enable = binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()
                binding.signInOutButton.isEnabled = enable
                binding.signUpButton.isEnabled = enable


            }
        }

        fragmentMyPageBinding.passwordEditText.addTextChangedListener{
            binding?.let { binding ->
                val enable = binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()
                binding.signInOutButton.isEnabled = enable
                binding.signUpButton.isEnabled = enable
            }
        }
    }

    override fun onStart() {
        super.onStart()

        //로그인이 풀려있는지 안풀려있는지를 확인하는 예외처리
        if(auth.currentUser == null) {
            binding?.let { binding ->
                binding.emailEditText.text.clear()
                binding.passwordEditText.text.clear()
                binding.emailEditText.isEnabled = true
                binding.passwordEditText.isEnabled = true
                binding.signInOutButton.text = "로그인"
                binding.signInOutButton.isEnabled = false
                binding.signUpButton.isEnabled = false
            }
        } else {
            binding?.let { binding ->
                binding.emailEditText.setText(auth.currentUser!!.email)
                binding.passwordEditText.setText("***********")
                binding.emailEditText.isEnabled = false
                binding.passwordEditText.isEnabled = false
                binding.signInOutButton.text = "로그아웃"
                binding.signInOutButton.isEnabled = true
                binding.signUpButton.isEnabled = false
            }
        }
    }

    private fun successSignIn() {
        //프래그먼트이므로 닫을 수가 없음
        //email, password editText를 잠그고 회원가입버튼 잠그고 로그인버튼->로그아웃버튼으로
        if (auth.currentUser == null) {
            Toast.makeText(context,"로그인에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        binding?.emailEditText?.isEnabled = false
        binding?.passwordEditText?.isEnabled = false
        binding?.signUpButton?.isEnabled = false
        binding?.signInOutButton?.text = "로그아웃"
        Toast.makeText(context,"로그인에 성공했습니다.", Toast.LENGTH_SHORT).show()
    }

}